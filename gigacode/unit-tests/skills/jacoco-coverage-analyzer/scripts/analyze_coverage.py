#!/usr/bin/env python3
"""
jacoco-coverage-analyzer: Анализ отчёта JaCoCo и вывод топ-10 классов с наименьшим покрытием
Usage: python3 analyze_coverage.py <project_path> [project_name]
"""

import sys
import os
import xml.etree.ElementTree as ET
import json

def find_jacoco_xml(project_path):
    """Ищет файл jacoco.xml в типичных местах Maven-проекта"""
    possible_paths = [
        os.path.join(project_path, "target", "site", "jacoco", "jacoco.xml"),
        os.path.join(project_path, "target", "jacoco.exec"),
    ]
    
    for path in possible_paths:
        if os.path.exists(path) and path.endswith(".xml"):
            return path
    return None

def parse_coverage(jacoco_xml_path):
    """Парсит XML отчёт и извлекает данные о покрытии классов"""
    tree = ET.parse(jacoco_xml_path)
    root = tree.getroot()
    
    classes = []
    for cls in root.findall(".//class"):
        name = cls.get("name", "")
        src = cls.get("sourcefilename", "unknown")
        
        # Используем INSTRUCTION counters
        missed = covered = 0
        for c in cls.findall("counter"):
            if c.get("type") == "INSTRUCTION":
                missed = int(c.get("missed", "0"))
                covered = int(c.get("covered", "0"))
                break
        
        total = missed + covered
        coverage = (covered / total * 100) if total > 0 else 100
        
        classes.append({
            "name": name.split(".")[-1],  # Только имя класса
            "source": src,
            "coverage": round(coverage, 2),
            "missed": missed,
            "covered": covered
        })
    
    # Сортировка по покрытию (возрастание)
    classes.sort(key=lambda x: x["coverage"])
    return classes

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 analyze_coverage.py <project_path> [project_name]")
        sys.exit(1)
    
    project_path = sys.argv[1]
    project_name = sys.argv[2] if len(sys.argv) > 2 else os.path.basename(project_path)
    
    # Если путь не абсолютный, строим относительно IdeaProjects
    if not os.path.isabs(project_path):
        if not project_path.startswith(("./", "../")):
            project_path = os.path.join(os.path.expanduser("~/IdeaProjects"), project_path)
    
    project_path = os.path.abspath(project_path)
    
    if not os.path.isdir(project_path):
        print(f"❌ Директория не найдена: {project_path}")
        sys.exit(1)
    
    # Ищем jacoco.xml
    jacoco_xml = find_jacoco_xml(project_path)
    if not jacoco_xml:
        # Пробуем найти в поддиректориях
        for root, dirs, files in os.walk(project_path):
            if "jacoco.xml" in files:
                jacoco_xml = os.path.join(root, "jacoco.xml")
                break
    
    if not jacoco_xml:
        print(f"❌ Файл jacoco.xml не найден в проекте: {project_path}")
        print("Сначала сгенерируйте отчёт: mvn jacoco:report")
        sys.exit(1)
    
    print(f"=== Анализ покрытия кода (JaCoCo) ===")
    print(f"Project: {project_path} ({project_name})")
    print(f"Отчёт: {jacoco_xml}")
    print()
    
    # Парсим отчёт
    classes = parse_coverage(jacoco_xml)
    
    if not classes:
        print("⚠️  В проекте не найдено классов с покрытием")
        sys.exit(0)
    
    total_classes = len(classes)
    avg_coverage = round(sum(c["coverage"] for c in classes) / total_classes, 2) if total_classes > 0 else 0
    
    print(f"Всего классов: {total_classes}")
    print(f"Среднее покрытие: {avg_coverage}%")
    print()
    print("ТОП-10 КЛАССОВ С НАИМЕНЬШИМ ПОКРЫТИЕМ:")
    print("-" * 50)
    
    # Топ-10 с наименьшим покрытием
    top_low = classes[:10]
    for i, cls in enumerate(top_low, 1):
        print(f"{i:2}. {cls['coverage']:7.2f}% | {cls['name']}")
    
    print()

if __name__ == "__main__":
    main()
