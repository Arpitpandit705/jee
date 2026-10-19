#!/usr/bin/env python3
"""Build app/src/main/assets/studyplan/plan.json

Merges:
  - artifacts/syllabus.json  (courses/chapters/lectures from PW API)
  - artifacts/progress.json  (user's reported completion)
  - artifacts/exclusions.json (removed misc topics)
  - live PW API DPP PDF attachments per subject/chapter (fetched here)

Output is a self-contained, SECRET-FREE plan the Android app imports on first run.
"""
import json, os, time, urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_ASSETS = os.path.join(ROOT, "app", "src", "main", "assets", "studyplan")

TOK = open(os.path.join(ROOT, ".env")).read().split("PW_AUTH_TOKEN=")[1].split("\n")[0]

API = "https://api.penpencil.co"
HEADERS = {
    "Authorization": "Bearer " + TOK,
    "Accept": "application/json",
    "Referer": "https://www.pw.live/",
}


def get(url, retries=3):
    last = None
    for _ in range(retries):
        try:
            req = urllib.request.Request(url, headers=HEADERS)
            with urllib.request.urlopen(req, timeout=30) as r:
                return json.load(r)
        except Exception as e:  # noqa: BLE001 - retry then raise
            last = e
            time.sleep(2)
    raise RuntimeError(f"GET failed: {url}: {last}")


PLAYLISTS = {
    "physics": "https://youtube.com/playlist?list=PLMttXSB2G2YPwvxukgg1e349zCanYZhsW",
    "chemistry": "https://youtube.com/playlist?list=PLxyGaR3hEy3hpXYV-iwKkj1lT1iyultEP",
    "maths": "https://youtube.com/playlist?list=PLxyGaR3hEy3hlHeokj5am7gON43w-NVOT",
}


def subject_playlist(group):
    return PLAYLISTS.get(group, PLAYLISTS["chemistry"])


def playlist_group(course_key):
    k = course_key.lower()
    if "physics" in k:
        return "physics"
    if "maths" in k:
        return "maths"
    return "chemistry"


def fetch_dpp_map(batch_slug, subject_slug):
    """Fetch all DppNotes for a subject; map topic id -> list of DPP PDFs."""
    topic_map = {}
    total = 0
    page = 1
    while page <= 25:
        d = get(f"{API}/v2/batches/{batch_slug}/subject/{subject_slug}/contents?page={page}&contentType=DppNotes")
        items = d.get("data") if isinstance(d.get("data"), list) else []
        if not items:
            break
        for c in items:
            for hw in c.get("homeworkIds") or []:
                topic = hw.get("topic")
                if not topic:
                    continue
                topic_id = topic if isinstance(topic, str) else (topic.get("_id") if isinstance(topic, dict) else None)
                if not topic_id:
                    continue
                for att in hw.get("attachmentIds") or []:
                    key = att.get("key")
                    if not key:
                        continue
                    url = (att.get("baseUrl") or "https://static.pw.live/").rstrip("/") + "/" + key
                    topic_map.setdefault(topic_id, []).append({
                        "name": att.get("name") or "DPP.pdf",
                        "url": url,
                        "order": len(topic_map.get(topic_id, [])),
                    })
                total += 1
        pages_seen = len(items)
        if pages_seen < 20:
            break
        page += 1
        time.sleep(0.3)
    return topic_map, total


def main():
    syl = json.load(open(os.path.join(ROOT, "artifacts", "syllabus.json")))
    prog = json.load(open(os.path.join(ROOT, "artifacts", "progress.json")))
    excl = json.load(open(os.path.join(ROOT, "artifacts", "exclusions.json")))

    excluded_ids = {e["topic_id"] for e in excl}

    # topic_id -> completed lecture count (from progress)
    done_map = {}
    for c in prog["courses"]:
        for cd in c.get("chapters_done", []):
            if isinstance(cd, dict) and "topic_id" in cd:
                done_map[cd["topic_id"]] = cd["total_lectures"]
        for pa in c.get("partial", []):
            done_map[pa["topic_id"]] = pa["completed_lecture_count"]

    plan = {"version": 3, "generated": "2026-10-18", "courses": []}

    for c in syl["courses"]:
        chapters_out = []
        lectures_total = lectures_done = 0

        if "11th basics" in c["course"]:
            # Outside snapshot: covered fully with Saleem Sir; chapter-level only.
            plan["courses"].append({
                "key": c["course"],
                "source": "Coaching Plus (Saleem Sir, 11th)",
                "subjectGroup": "physics",
                "playlistUrl": subject_playlist("physics"),
                "fullyComplete": True,
                "chapters": [],
            })
            continue

        batch_slug, subject_slug = c["batch_slug"], c["subject_slug"]
        print(f"[{c['course']}] fetching DPP PDFs...", flush=True)
        dpp_map, dpp_seen = fetch_dpp_map(batch_slug, subject_slug)
        print(f"   DPP docs seen: {dpp_seen}, mapped topics: {len(dpp_map)}", flush=True)

        for t in c["chapters"]:
            if t["topic_id"] in excluded_ids or not t["lectures"]:
                continue
            done_n = min(done_map.get(t["topic_id"], 0), len(t["lectures"]))
            dpps = dpp_map.get(t["topic_id"], [])
            chapter_lectures = []
            for idx, l in enumerate(t["lectures"]):
                is_done = idx < done_n
                chapter_lectures.append({
                    "pwId": l["id"],
                    "title": l["name"],
                    "date": l["date"],
                    "durationMin": l["duration_min"],
                    "isCompleted": is_done,
                })
            lectures_total += len(chapter_lectures)
            lectures_done += sum(1 for l in chapter_lectures if l["isCompleted"])
            chapters_out.append({
                "topicId": t["topic_id"],
                "name": t["name"],
                "dppCount": len(dpps),
                "dpps": dpps[:6],  # cap: keep newest 6 PDF links per chapter
                "lectures": chapter_lectures,
            })

        plan["courses"].append({
            "key": c["course"],
            "batchSlug": batch_slug,
            "subjectSlug": subject_slug,
            "source": "Physics Wallah API",
            "subjectGroup": playlist_group(c["course"]),
            "playlistUrl": subject_playlist(playlist_group(c["course"])),
            "fullyComplete": False,
            "lectureTotal": lectures_total,
            "lectureDone": lectures_done,
            "chapters": chapters_out,
        })
        print(f"[{c['course']}] chapters={len(chapters_out)} lectures={lectures_total} done={lectures_done}", flush=True)

    os.makedirs(OUT_ASSETS, exist_ok=True)
    out = os.path.join(OUT_ASSETS, "plan.json")
    with open(out, "w") as f:
        json.dump(plan, f, indent=1, ensure_ascii=False)

    # secret-free sanity check
    raw = open(out).read()
    for bad in ("PW_AUTH_TOKEN", "eyJ", "GEMINI_API_KEY"):
        if bad in raw:
            raise SystemExit(f"SECURITY ERROR: plan.json contains '{bad}'!")

    size = os.path.getsize(out)
    print(f"plan.json OK -> {out} ({size/1024:.0f} KB)")


if __name__ == "__main__":
    main()
