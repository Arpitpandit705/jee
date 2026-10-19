#!/usr/bin/env python3
"""Build artifacts/progress.json — user's reported study completion mapped to syllabus.json.

User status (2026-10-18):
  - Physics 11th basics  : Saleem Sir (Coaching Plus) Ch-01..Ch-07 COMPLETE (tracked chapter-level)
  - Physics 11th tail    : NKC Sir (Prayas 2.0 2026) Rotational Motion -> Waves : NOT started
  - Physics 12th         : Saleem Sir (Coaching Plus) Ch-15+ : NOT started
  - Maths (Sachin Sir)   : Ch-01..Ch-05 COMPLETE; Binomial Theorem: 2 of 3 lectures
  - Phys Chem (Faisal)   : Some Basic Concepts + Redox + Solutions COMPLETE; Chemical Kinetics: 2 lectures
  - Inorganic (Om Pandey): Periodic Table COMPLETE; Chemical Bonding: 3 lectures
  - Organic (Rohit)      : IUPAC Nomenclature: 3 of 10 lectures
"""
import json
import os
import urllib.request

SYL = "artifacts/syllabus.json"
OUT = "artifacts/progress.json"

TOK = open(".env").read().split("PW_AUTH_TOKEN=")[1].split("\n")[0]


def saleem_topic(key):
    """Chapter-level entry from Saleem Sir's FULL topic list (Coaching Plus, includes Ch-01..07)."""
    slug = "physics-by-saleem-sir--2025--297330"
    topics = []
    for p in range(1, 10):
        req = urllib.request.Request(
            f"https://api.penpencil.co/v2/batches/jee-coaching-plus-2026-663976/subject/{slug}/topics?page={p}",
            headers={"Authorization": "Bearer " + TOK, "Accept": "application/json", "Referer": "https://www.pw.live/"},
        )
        ts = json.load(urllib.request.urlopen(req, timeout=30)).get("data")
        if not ts:
            break
        topics += ts
    for t in topics:
        if key.lower() in t["name"].lower() and t.get("videos", 0):
            return {"topic_id": t["_id"], "chapter": t["name"], "total_lectures": t["videos"], "source": "outside-snapshot (complete)"}
    raise KeyError(key)

data = json.load(open(SYL))
courses = {c["course"]: c for c in data["courses"]}


def find(cname, key):
    for t in courses[cname]["chapters"]:
        if key.lower() in t["name"].lower() and t["lectures"]:
            return {"topic_id": t["topic_id"], "chapter": t["name"], "total_lectures": len(t["lectures"])}
    raise KeyError(f"{cname}: {key}")


prog = {
    "updated": "2026-10-18",
    "yt_playlists": {
        "physics": "https://youtube.com/playlist?list=PLMttXSB2G2YPwvxukgg1e349zCanYZhsW",
        "chemistry": "https://youtube.com/playlist?list=PLxyGaR3hEy3hpXYV-iwKkj1lT1iyultEP",
        "maths": "https://youtube.com/playlist?list=PLxyGaR3hEy3hlHeokj5am7gON43w-NVOT",
        "channel_reference": "https://youtube.com/@physicswallah",
        "channel_note": "General channel - subject playlists already attached, so channel link is optional backup",
    },
    "courses": [
        {
            "course": "Physics (11th basics)",
            "source": "Covered from Saleem Sir's 11th version (Coaching Plus), outside snapshot",
            "status": "COMPLETE",
            "chapters_done": [
                saleem_topic(n)
                for n in ["Mathematical Tools", "Motion In Straight Line", "Motion In A Plane",
                          "Laws Of Motion", "Circular Motion", "Work, Energy & Power", "Centre of Mass"]
            ],
            "chapters_done_note": "Chapter names matched against Saleem Sir's full topic list; lecture-level data not in snapshot (all complete).",
        },
        {
            "course": "Physics (11th tail) (NKC Sir)",
            "source": "Prayas JEE 2.0 2026",
            "status": "NOT_STARTED",
            "chapters_done": [],
        },
        {
            "course": "Physics (12th) (Saleem Sir)",
            "source": "JEE Coaching Plus 2026",
            "status": "NOT_STARTED",
            "chapters_done": [],
        },
        {
            "course": "Physical Chemistry (Faisal Razaq Sir)",
            "source": "Prayas 2027",
            "status": "IN_PROGRESS",
            "chapters_done": [
                find("Physical Chemistry (Faisal Razaq Sir)", "Some Basic Concepts"),
                find("Physical Chemistry (Faisal Razaq Sir)", "Redox"),
                find("Physical Chemistry (Faisal Razaq Sir)", "Solutions"),
            ],
            "partial": [
                {**find("Physical Chemistry (Faisal Razaq Sir)", "Chemical Kinetics"),
                 "completed_lecture_count": 2,
                 "assumption": "first 2 lectures in batch-date order"},
            ],
        },
        {
            "course": "Inorganic Chemistry (Om Pandey Sir)",
            "source": "Prayas JEE 2.0 2026",
            "status": "IN_PROGRESS",
            "chapters_done": [
                find("Inorganic Chemistry (Om Pandey Sir)", "Periodic Table"),
            ],
            "partial": [
                {**find("Inorganic Chemistry (Om Pandey Sir)", "Chemical Bonding"),
                 "completed_lecture_count": 3,
                 "assumption": "first 3 lectures in batch-date order"},
            ],
        },
        {
            "course": "Organic Chemistry (Rohit Agarwal Sir)",
            "source": "JEE Coaching Plus 2026",
            "status": "IN_PROGRESS",
            "chapters_done": [],
            "partial": [
                {**find("Organic Chemistry (Rohit Agarwal Sir)", "IUPAC Nomenclature"),
                 "completed_lecture_count": 3,
                 "assumption": "first 3 lectures in batch-date order"},
            ],
        },
        {
            "course": "Maths (Sachin Jakhar Sir)",
            "source": "JEE Coaching Plus 2026",
            "status": "IN_PROGRESS",
            "chapters_done": [
                find("Maths (Sachin Jakhar Sir)", "Basic Maths"),
                find("Maths (Sachin Jakhar Sir)", "Quadratic"),
                find("Maths (Sachin Jakhar Sir)", "Sequence"),
                find("Maths (Sachin Jakhar Sir)", "Trigonometric Functions"),
                find("Maths (Sachin Jakhar Sir)", "Trigonometric Equations"),
            ],
            "partial": [
                {**find("Maths (Sachin Jakhar Sir)", "Binomial Theorem"),
                 "completed_lecture_count": 2,
                 "assumption": "first 2 of 3 in batch-date order"},
            ],
        },
    ],
}

json.dump(prog, open(OUT, "w"), indent=1, ensure_ascii=False)

# stats
done = 0
for c in prog["courses"]:
    for cd in c.get("chapters_done", []):
        if isinstance(cd, dict) and "total_lectures" in cd:
            done += cd["total_lectures"]
    for pa in c.get("partial", []):
        done += pa["completed_lecture_count"]
print(f"progress.json saved -> {OUT}")
print(f"Lectures completed (snapshot-tracked): {done} of 586")
print("(+ 11th Physics basics Ch-01..07 ke ~70 lectures Saleem Sir se — snapshot se bahar)")
