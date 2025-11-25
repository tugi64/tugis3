#!/usr/bin/env python3
import subprocess
import sys
import shutil
from datetime import datetime

def run_git_command(args):
    """Run git command and return output"""
    try:
        result = subprocess.run(
            ['git'] + args,
            cwd=r'C:\Users\CASPER\AndroidStudioProjects\tugis3',
            capture_output=True,
            text=True,
            encoding='utf-8',
            errors='replace'
        )
        return result.stdout, result.stderr, result.returncode
    except Exception as e:
        return '', str(e), 1

print("="*70)
print("Git Restore Tool - LineStakeoutActivity.kt Recovery")
print("="*70)

# Backup current file
current_file = r'C:\Users\CASPER\AndroidStudioProjects\tugis3\app\src\main\java\com\example\tugis3\ui\survey\LineStakeoutActivity.kt'
backup_file = r'C:\Users\CASPER\AndroidStudioProjects\tugis3\LineStakeoutActivity_BROKEN_BACKUP.kt'

print("\n1. Backing up current (broken) file...")
try:
    shutil.copy2(current_file, backup_file)
    print(f"   ✓ Backup saved to: {backup_file}")
except Exception as e:
    print(f"   ✗ Backup failed: {e}")

# Get latest commit where the file exists
print("\n2. Finding latest working commit...")
stdout, stderr, code = run_git_command([
    'log', '-1', '--oneline',
    '--', 'app/src/main/java/com/example/tugis3/ui/survey/LineStakeoutActivity.kt'
])

if code == 0 and stdout:
    commit_hash = stdout.split()[0]
    commit_msg = ' '.join(stdout.split()[1:])
    print(f"   ✓ Found commit: {commit_hash} - {commit_msg}")

    print(f"\n3. Restoring file from commit {commit_hash}...")
    file_content, err, code = run_git_command([
        'show',
        f'{commit_hash}:app/src/main/java/com/example/tugis3/ui/survey/LineStakeoutActivity.kt'
    ])

    if code == 0:
        # Save to current file location
        try:
            with open(current_file, 'w', encoding='utf-8') as f:
                f.write(file_content)
            print(f"   ✓ SUCCESS! File restored from commit {commit_hash}")
            print(f"   ✓ LineStakeoutActivity.kt has been restored")
            print(f"\n" + "="*70)
            print("RESTORATION COMPLETE!")
            print("="*70)
            print("\nWhat was done:")
            print(f"  • Broken file backed up to: {backup_file}")
            print(f"  • File restored from commit: {commit_hash}")
            print(f"  • Commit date: {commit_msg}")
            print("\nNext steps:")
            print("  1. Rebuild the project in Android Studio")
            print("  2. If this version doesn't work, check Local History in IDE")
            print("  3. Your broken version is safely backed up")
            print("="*70)
        except Exception as e:
            print(f"   ✗ Error writing file: {e}")
    else:
        print(f"   ✗ Error retrieving file: {err}")
else:
    print(f"   ✗ Error finding commit: {stderr}")

print("\n\nPress any key to exit...")
input()

