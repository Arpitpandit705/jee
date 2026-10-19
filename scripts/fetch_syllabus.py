#!/usr/bin/env python3
"""
FocusStudy - PW (Physics Wallah) syllabus snapshot fetcher.

Fetches the agreed course mapping from api.penpencil.co and saves
a structured JSON snapshot (artifacts/syllabus.json):

Course mapping (locked with user, 2026-10-18):
  - Physics (12th)   : JEE Coaching Plus 2026 -> "Physics by Saleem Sir (2025)", chapters from Ch-15 (Electric Charges & Fields) onward
  - Physics (11th)   : Prayas JEE 2.0 2026 -> Physics, chapters Ch-08 (Rotational Motion) .. Ch-14 (Waves)
  - Physical Chem    : Prayas 2027 -> Physical Chemistry (Faisal Razaq Sir)
  - Inorganic Chem   : Prayas JEE 2.0 2026 -> Inorganic Chemistry (Om Pandey Sir)
  - Organic Chem     : JEE Coaching Plus 2026 -> "Organic Chemistry By Rohit Agarwal Sir"
  - Maths            : JEE Coaching Plus 2026 -> "Maths By Sachin Jakhar Sir"

Token is read from .env (PW_AUTH_TOKEN). Output: artifacts/syllabus.json
"""

import json
import os
import re
import sys
import urllib.request
import urllib.error

API = "https://api.penpencil.co"
OUT = os.path.join(os.path.dirname(__file__), "..", "artifacts", "syllabus.json")
BATCH_SLUG_CP = "jee-coaching-plus-2026-663976"
BATCH_SLUG_P27 = "prayas-2027-746901"
BATCH_SLUG_P26 = "prayas-jee-2-0-2026-222936"

# (course_key, batch_slug, subject_slug, filter mode, arg)
# filter mode: "regex" excludes chapters before match start (12th start), "range" includes chapter span, "all" takes everything
MAPPING = [
    ("Physics (12th)", BATCH_SLUG_CP, "physics-by-saleem-sir--2025--297330", "regex", r"^Ch- ?15"),
    ("Physics (11th)", BATCH_SLUG_P26, "physics-168894", "range", (r"^Ch\s*-?\s*0?8\b", r"^Ch\s*-?\s*14\b")),
    ("Physical Chemistry (Faisal Razaq Sir)", BATCH_SLUG_P27, "physical-chemistry-186128", "all", None),
    ("Inorganic Chemistry (Om Pandey Sir)", BATCH_SLUG_P26, "inorganic-chemistry-956522", "all", None),
    ("Organic Chemistry (Rohit Agarwal Sir)", BATCH_SLUG_CP, "organic-chemistry-by-rohit-agarwal-sir-424778", "all", None),
    ("Maths (Sachin Jakhar Sir)", BATCH_SLUG_CP, "maths-by-sachin-jakhar-sir-767518", "all", None),
]


def load_token():
    env = os.path.join(os.path.dirname(__file__), "..", ".env")
    with open(env) as f:
        for line in f:
            if line.startswith("PW_AUTH_TOKEN="):
                return line.strip().split("=", 1)[1]
    raise RuntimeError("PW_AUTH_TOKEN not found in .env")


TOK = load_token()


def get(url, retries=3):
    for i in range(retries):
        try:
            req = urllib.request.Request(
                url,
                headers={
                    "Authorization": "Bearer " + TOK,
                    "Accept": "application/json",
                    "Referer": "https://www.pw.live/",
                },
            )
            with urllib.request.urlopen(req, timeout=30) as r:
                return json.load(r)
        except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError) as e:
            if i == retries - 1:
                raise
    return None


def parse_dur(d):
    """'00:58:34' -> minutes (int)"""
    if not d:
        return 0
    parts = str(d).split(":")
    try:
        if len(parts) == 3:
            h, m, s = map(int, parts)
            return h * 60 + m + (1 if s >= 30 else 0)
        if len(parts) == 2:
            m, s = map(int, parts)
            return m + (1 if s >= 30 else 0)
    except ValueError:
        return 0
    return 0


def fetch_topics(batch, subject):
    topics = []
    for p in range(1, 40):
        d = get(f"{API}/v2/batches/{batch}/subject/{subject}/topics?page={p}")
        ts = d.get("data") if (d and isinstance(d.get("data"), list)) else []
        if not ts:
            break
        topics += ts
    return topics


def select_topics(topics, mode, arg):
    def chap_num(name):
        m = re.match(r"^Ch[-\s]*\s?(\d+)", name.strip(), re.I)
        return int(m.group(1)) if m else None

    out = []
    if mode == "all":
        out = topics
    elif mode == "regex":
        start = None
        for t in topics:
            if re.search(arg, t["name"], re.I):
                start = chap_num(t["name"])
                break
        if start is None:
            sys.exit(f"ERROR: start chapter pattern {arg} not found")
        out = [t for t in topics if (chap_num(t["name"]) is not None and chap_num(t["name"]) >= start)]
    elif mode == "range":
        a, b = arg
        first = next((chap_num(t["name"]) for t in topics if re.search(a, t["name"], re.I)), None)
        last = next((chap_num(t["name"]) for t in topics if re.search(b, t["name"], re.I)), None)
        if first is None or last is None:
            sys.exit(f"ERROR: range patterns {arg} not found in topics")
        out = [t for t in topics if chap_num(t["name"]) is not None and first <= chap_num(t["name"]) <= last]
    # keep only real chapter topics (have videos, start with Ch- or mode==all keeps video topics)
    return out


def fetch_lectures(batch, subject, topic=None):
    """Page through video contents for a subject; optionally filter by topic id."""
    lectures = []
    for p in range(1, 120):
        q = f"page={p}&contentType=videos"
        if topic:
            q += f"&tag={topic}"
        d = get(f"{API}/v2/batches/{batch}/subject/{subject}/contents?{q}")
        items = d.get("data") if (d and isinstance(d.get("data"), list)) else []
        if not items:
            break
        for c in items:
            vd = c.get("videoDetails") or {}
            if not vd.get("name"):
                continue
            lectures.append(
                {
                    "id": vd.get("_id") or c.get("_id"),
                    "name": vd.get("name"),
                    "date": (c.get("date") or "")[:10],
                    "duration_min": parse_dur(vd.get("duration")),
                }
            )
        if len(items) < 20:
            break
    return lectures


def main():
    snapshot = {"generated": "2026-10-18", "batch_slugs": [BATCH_SLUG_CP, BATCH_SLUG_P27, BATCH_SLUG_P26], "courses": []}
    for course, batch, subject, mode, arg in MAPPING:
        print(f"[{course}] topics fetch...", flush=True)
        topics = fetch_topics(batch, subject)
        chosen = select_topics(topics, mode, arg)
        if not chosen:
            sys.exit(f"ERROR: no chapters selected for {course}")
        chapters = []
        total = 0
        total_min = 0
        for t in chosen:
            lec = fetch_lectures(batch, subject, topic=t["_id"]) if t.get("videos", 0) else []
            lec.sort(key=lambda x: (x["date"], x["name"]))
            chapters.append({"topic_id": t["_id"], "name": t["name"], "lectures": lec})
            total += len(lec)
            total_min += sum(l["duration_min"] for l in lec)
            print(f"   {t['name']}: {len(lec)} lectures", flush=True)
        snapshot["courses"].append(
            {
                "course": course,
                "batch_slug": batch,
                "subject_slug": subject,
                "chapters": chapters,
                "summary": {"chapters": len(chapters), "lectures": total, "total_minutes": total_min},
            }
        )
        print(f"[{course}] DONE -> {len(chapters)} chapters, {total} lectures, {total_min//60}h", flush=True)

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w") as f:
        json.dump(snapshot, f, indent=1, ensure_ascii=False)
    print("Saved:", OUT)


if __name__ == "__main__":
    main()
