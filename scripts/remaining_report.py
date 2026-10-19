#!/usr/bin/env python3
"""Compute remaining lectures per chapter and per subject.
Reads artifacts/syllabus.json + artifacts/progress.json, prints report,
and saves artifacts/remaining.json."""
import json

syl = json.load(open("artifacts/syllabus.json"))
prog = json.load(open("artifacts/progress.json"))

# exclusions: misc topics user marked useless
excluded_ids = {e["topic_id"] for e in json.load(open("artifacts/exclusions.json"))}

# build completion map: topic_id -> lectures completed
done_map = {}  # topic_id -> count
for c in prog["courses"]:
    for cd in c.get("chapters_done", []):
        if isinstance(cd, dict) and "topic_id" in cd:
            done_map[cd["topic_id"]] = cd["total_lectures"]  # full chapter done
    for pa in c.get("partial", []):
        done_map[pa["topic_id"]] = pa["completed_lecture_count"]

report = {"updated": "2026-10-18", "excluded_topics": len(excluded_ids), "subjects": []}
grand_total = grand_left = 0

for c in syl["courses"]:
    rows = []
    subj_done = subj_left = subj_total = 0
    for t in c["chapters"]:
        total = len(t["lectures"])
        if total == 0 or t["topic_id"] in excluded_ids:
            continue  # PDF-only or excluded topics skip
        done = min(done_map.get(t["topic_id"], 0), total)
        left = total - done
        subj_total += total
        subj_done += done
        subj_left += left
        mark = "✅" if left == 0 else ("🟡" if done else "⬜")
        extra = f"  ({done}/{total} done)" if done and left else (f" ({total} done)" if left == 0 else "")
        rows.append((mark, t["name"], total, done, left, extra))
    grand_total += subj_total
    grand_left += subj_left
    report["subjects"].append({
        "course": c["course"],
        "total_lectures": subj_total,
        "completed": subj_done,
        "remaining": subj_left,
        "chapters": [{"chapter": r[1], "total": r[2], "done": r[3], "remaining": r[4]} for r in rows],
    })
    print(f"\n{'='*70}")
    print(f"📚 {c['course']}  —  {subj_done}/{subj_total} done, {subj_left} LEFT")
    print(f"{'='*70}")
    for mark, name, total, done, left, extra in rows:
        print(f"  {mark} {name[:58]:58} {left:>3} bache{extra}")

report["TOTAL"] = {"total_lectures": grand_total, "completed": grand_total - grand_left, "remaining": grand_left}
print(f"\n{'#'*70}")
print(f"🎯 TOTAL: {grand_total} me se {grand_total-grand_left} done, {grand_left} LECTURES BACHE")
print(f"{'#'*70}")

json.dump(report, open("artifacts/remaining.json", "w"), indent=1, ensure_ascii=False)
print("Saved: artifacts/remaining.json")
