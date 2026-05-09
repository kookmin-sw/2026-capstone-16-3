---
title: "길벗"
layout: splash
permalink: /

header:
  overlay_image: /assets/images/demo.png
  # overlay_color: "#000"
  overlay_filter: "0.55"
  caption: "AI Safe Walking Assistant"

excerpt: >
  시각장애인 및 보행약자를 위한 AI 기반 안전 보행 보조 플랫폼

feature_row:
  - title: "장애물 탐지"
    excerpt: "카메라 기반 객체 탐지로 킥보드, 차량, 볼라드 등 보행 위험 요소를 인식한다."
  - title: "횡단보도 안내"
    excerpt: "횡단보도, 신호등, 음향신호기 정보를 활용해 안전한 횡단을 보조한다."
  - title: "음성·진동 안내"
    excerpt: "TTS와 햅틱 피드백으로 화면 확인 없이 위험 상황과 이동 방향을 전달한다."
---

<!-- markdownlint-disable MD025 MD033 -->

# 길벗

길벗은 스마트폰 카메라와 AI 기반 객체 탐지 기술을 활용하여  
시각장애인과 보행약자의 안전한 실외 보행을 지원하는 플랫폼이다.

{% include feature_row %}

## 목차

1. [주요 기능](#주요-기능)
1. [소개 영상](#소개-영상)
1. [팀원 소개](#팀원-소개)
1. [기술 스택](#기술-스택)
1. [사용법](#사용법)
1. [폴더 구조](#폴더-구조)
1. [GitHub](#github)

## 주요 기능

- 보도 장애물 탐지
- 점자블록 및 보행 가능 영역 분석
- 횡단보도 및 신호등 상태 인식
- 음향신호기 데이터 연동
- TTS 기반 음성 안내
- 진동 기반 위험 피드백
- 목적지 기반 보행 경로 안내

## 소개 영상

{% include section/demo.html %}

## 팀원 소개

{% include section/team.html %}

## 기술 스택

{% include section/architecture.html %}

## 사용법

{% include section/guide.html %}

## 폴더 구조

```md
2026-capstone-16/
├── frontend/                
│   ├── android/
│   ├── ios/
│   ├── assets/
│   ├── lib/
│   ├── pubspec.lock
│   └── pubspec.yaml
│
├── server/                 
│   ├── src/
│   ├── gradle/
│   ├── build.gradle
│   ├── settings.gradle
│   ├── gradlew
│   ├── gradlew.bat
│   └── Dockerfile
│
├── ai/                   
│   ├── app/
│   ├── models/
│   └── requirements.txt
│
├── docs/                    
│   ├── _pages/
│   ├── _layouts/
│   ├── _includes/
│   ├── _sass/
│   ├── _data/
│   ├── assets/
│   └── index.md
│
└── README.md
```

## GitHub

[프로젝트 저장소](https://github.com/kookmin-sw/2026-capstone-16)
