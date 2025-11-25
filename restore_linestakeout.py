import subprocess
import os

os.chdir(r"C:\Users\CASPER\AndroidStudioProjects\tugis3")

print("Checking git history for LineStakeoutActivity.kt...")

# Get commits from 2025-10-27
result = subprocess.run([
    "git", "log", "--all",
    "--since=2025-10-27 20:00",
    "--until=2025-10-27 23:59",
    "--pretty=format:%H|%ad|%s",
    "--date=format:%Y-%m-%d %H:%M",
    "--", "app/src/main/java/com/example/tugis3/ui/survey/LineStakeoutActivity.kt"
], capture_output=True, text=True, encoding='utf-8')

commits = result.stdout.strip().split('\n')

print(f"\nFound {len(commits)} commits on 2025-10-27:")
for i, commit in enumerate(commits):
    if commit:
        parts = commit.split('|')
        if len(parts) >= 3:
            print(f"{i+1}. [{parts[0][:8]}] {parts[1]} - {parts[2]}")

if commits and commits[0]:
    # Get the last commit from that day (closest to 22:40)
    commit_hash = commits[0].split('|')[0]

    print(f"\nRestoring from commit: {commit_hash[:8]}...")

    # Get file content from that commit
    result = subprocess.run([
        "git", "show", f"{commit_hash}:app/src/main/java/com/example/tugis3/ui/survey/LineStakeoutActivity.kt"
    ], capture_output=True, text=True, encoding='utf-8')

    if result.returncode == 0:
        # Write to new file
        with open(r"app\src\main\java\com\example\tugis3\ui\survey\LineStakeoutActivity.kt", 'w', encoding='utf-8') as f:
            f.write(result.stdout)

        print("✓ File restored successfully!")
        print("✓ LineStakeoutActivity.kt has been restored from 2025-10-27")
    else:
        print("✗ Error restoring file:")
        print(result.stderr)
else:
    print("\n✗ No commits found for that date.")
    print("Checking all recent commits...")

    result = subprocess.run([
        "git", "log", "--all", "-10",
        "--pretty=format:%H|%ad|%s",
        "--date=format:%Y-%m-%d %H:%M"
    ], capture_output=True, text=True, encoding='utf-8')

    print("\nRecent commits:")
    print(result.stdout)

