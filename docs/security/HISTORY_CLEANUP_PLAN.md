# Scoped Git History Cleanup Plan (Requires Explicit Owner Approval)

**Date:** 18 November 2026  
**Finding:** `SEC-01` (hardcoded test/admin credentials in tracked scripts under `merchant-app/src/scripts/`).  
**Status:** Source files cleaned in current working branch (`phase/s1-security`). Git history still retains prior commits (`4ee27a1` and earlier) containing those literal strings.

---

## 1. Mandatory Pre-Requisites (Do First — Before Any History Rewrite)

1. **Rotate Credentials First:** In Firebase Console -> Authentication -> Users for project `gen-lang-client-0615495350`, immediately reset passwords (or disable/delete unused test accounts) for all accounts previously referenced in `merchant-app/src/scripts/*.ts`. Check sign-in / audit history for unauthorized activity.
2. **Ensure No Reused Passwords:** If any exposed password was reused on other services, rotate those immediately.
3. **Create a Full Backup Mirror:**
   ```bash
   git clone --mirror https://github.com/rejeeshgr410/Deal-Finder.git Deal-Finder-backup.git
   ```
4. **Coordinate With Collaborators:** Confirm no other contributors have open branches/PRs based on old commit SHAs, as history rewriting changes every commit hash.

---

## 2. Scoped Rewrite Procedure (After Owner Approval)

1. Install `git-filter-repo` (`pip install git-filter-repo`).
2. Create a local expressions file (`secrets-to-scrub.txt`, kept **outside** the repository and never committed) mapping each exposed literal password/string to `***REDACTED_CREDENTIAL***`.
3. Run on a fresh clone:
   ```bash
   git filter-repo --replace-text ../secrets-to-scrub.txt
   ```
4. Verify no occurrences remain across all branches and tags:
   ```bash
   git log --all -S "<redacted-pattern>" --oneline
   ```
5. Re-attach `origin`, force-push cleaned branches/tags (`git push --force --all && git push --force --tags`), and have all local clones re-cloned cleanly.

---

## 3. Verification & Sign-Off
- [ ] Passwords rotated in Firebase Console
- [ ] Mirror backup verified
- [ ] `git-filter-repo` executed and verified on fresh clone
- [ ] Old local clones replaced; `secrets-to-scrub.txt` securely deleted
