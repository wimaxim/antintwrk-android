#!/usr/bin/env python3
"""Keep project-owned temporary resources when importing upstream translations."""
from pathlib import Path
from xml.dom import minidom


def preserve_temporary_resources(root):
    removed = 0
    for temporary in sorted(Path(root).glob("**/src/*/res/values*/temporary.xml")):
        names = {
            node.getAttribute("name")
            for node in minidom.parse(str(temporary)).documentElement.childNodes
            if node.nodeType == node.ELEMENT_NODE and node.hasAttribute("name")
        }
        for filename in ("localazy.xml", "translations.xml"):
            target = temporary.with_name(filename)
            if not target.exists():
                continue
            doc = minidom.parse(str(target))
            duplicates = [
                node for node in doc.documentElement.childNodes
                if node.nodeType == node.ELEMENT_NODE
                and node.getAttribute("name") in names
            ]
            if duplicates:
                for node in duplicates:
                    doc.documentElement.removeChild(node)
                    node.unlink()
                target.write_bytes(doc.toxml(encoding="utf-8"))
                removed += len(duplicates)
                print(f"{target.relative_to(root)}: preserved {len(duplicates)} temporary resources")
    return removed


if __name__ == "__main__":
    preserve_temporary_resources(Path(__file__).resolve().parents[2])
