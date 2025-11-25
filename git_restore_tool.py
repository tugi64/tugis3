#!/usr/bin/env python3
import subprocess
import sys

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

# Get recent commits
print("Fetching recent commits...")
stdout, stderr, code = run_git_command(['log', '--all', '-20', '--oneline', '--date=format:%Y-%m-%d %H:%M', '--pretty=format:%h|%ad|%s'])

if code == 0 and stdout:
    lines = stdout.strip().split('\n')
    print(f"\nFound {len(lines)} recent commits:\n")

    for i, line in enumerate(lines, 1):
        print(f"{i}. {line}")

    print("\n" + "="*60)
    print("To restore a specific commit, note its hash (first column)")
    print("="*60)

    # Try to get commits from Oct 27
    print("\n\nSearching for commits on 2025-10-27...")
    stdout27, _, code27 = run_git_command([
        'log', '--all',
        '--since=2025-10-27 00:00',
        '--until=2025-10-28 00:00',
        '--oneline',
        '--date=format:%Y-%m-%d %H:%M',
        '--pretty=format:%h|%ad|%s'
    ])

    if code27 == 0 and stdout27:
        lines27 = stdout27.strip().split('\n')
        print(f"Found {len(lines27)} commits on Oct 27:\n")
        for line in lines27:
            print(f"  {line}")

        if lines27 and lines27[0]:
            # Get the first commit hash
            commit_hash = lines27[0].split('|')[0]
            print(f"\n\nWould you like to restore from commit {commit_hash}? (y/n)")
            # For automation, let's get the file
            print(f"\nAttempting to restore LineStakeoutActivity.kt from {commit_hash}...")

            file_content, err, code = run_git_command([
                'show',
                f'{commit_hash}:app/src/main/java/com/example/tugis3/ui/survey/LineStakeoutActivity.kt'
            ])

            if code == 0:
                # Save to output file
                output_path = r'C:\Users\CASPER\AndroidStudioProjects\tugis3\LineStakeoutActivity_recovered.kt'
                with open(output_path, 'w', encoding='utf-8') as f:
                    f.write(file_content)
                print(f"\n✓ SUCCESS! File saved to: {output_path}")
                print(f"✓ Please review and replace the current file if correct.")
            else:
                print(f"\n✗ Error retrieving file: {err}")
    else:
        print("No commits found on 2025-10-27")
        print("The project might not have been committed that day.")
else:
    print(f"Error running git: {stderr}")

print("\n\nDone!")

