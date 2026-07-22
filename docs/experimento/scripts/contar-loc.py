import sys, re, os

def count_loc(path):
    with open(path, encoding="utf-8") as f:
        text = f.read()
    # strip block comments /* ... */ (including javadoc)
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.DOTALL)
    loc = 0
    for line in text.splitlines():
        s = line.strip()
        if not s:
            continue
        if s.startswith("//"):
            continue
        loc += 1
    return loc

def walk_total(root):
    total = 0
    for dirpath, _, files in os.walk(root):
        for fn in files:
            if fn.endswith(".java"):
                total += count_loc(os.path.join(dirpath, fn))
    return total

if __name__ == "__main__":
    owner_dir = "src/main/java/org/springframework/samples/petclinic/owner"
    rows = []
    for fn in sorted(os.listdir(owner_dir)):
        if fn.endswith(".java"):
            p = os.path.join(owner_dir, fn)
            rows.append((fn, count_loc(p)))
    for fn, loc in rows:
        print(f"{fn}\t{loc}")
    print("---")
    print("TOTAL owner pkg\t", sum(l for _, l in rows))
    print("TOTAL src/main\t", walk_total("src/main"))
