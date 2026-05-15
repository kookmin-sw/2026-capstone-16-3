<!-- markdownlint-disable MD025 MD033 MD036 -->

# <img src="docs/assets/images/icon.png" alt="길벗 아이콘" height="35" align="left">당신의 걸음 곁에, **"길벗"**

> 시각장애인과 저시력자가 보다 안전하고 독립적으로 이동할 수 있도록 돕는  
> **AI 기반 보행 보조 서비스**

<div align="center">
  <img src="docs/assets/images/preview.png" alt="길벗 앱 미리보기" width="60%">
</div>

## 🌐 프로젝트 소개 사이트

["길벗" 프로젝트 소개 사이트"](https://kookmin-sw.github.io/2026-capstone-16/)

## 🎬프로젝트 소개 영상

["길벗" 프로젝트 소개 영상](https://www.youtube.com/watch?v=9PLm9LIociU)

## 📌 목차

- [프로젝트 소개](#-프로젝트-소개)
- [주요 기능](#-주요-기능)
- [시스템 구조도](#️-시스템-구조도)
- [기술 스택](#️-기술-스택)
- [사용법](#-사용법)
- [폴더 구조](#-폴더-구조)
- [기대 효과](#-기대-효과)
- [팀원 소개](#-팀16-리트리버-소개)
- [참고 자료](#-참고-자료)

## 🌟 프로젝트 소개

본 프로젝트 **"길벗"** 은 시각장애인 및 저시력자의 안전한 보행을 지원하기 위한 **모바일 기반 보행 보조 애플리케이션** 을 개발하는 것을 목표로 합니다.

스마트폰 카메라와 인공지능 기반 객체 인식 기술을 활용하여 사용자의 주변 환경을 **실시간으로 분석**하고, 보행 중 발생할 수 있는 장애물을 탐지합니다. 단순히 장애물을 감지하는 것에 그치지 않고, 장애물의 위치를 분석하여 사용자에게 **최적의 회피 방향**을 안내합니다. 예를 들어 장애물이 사용자의 정면에 위치한 경우 *"왼쪽으로 이동하세요."* 와 같은 음성 안내를 제공하여 사용자가 안전하게 장애물을 피할 수 있도록 지원합니다.

또한, 사용자가 목적지를 입력하면 **음성 기반 길찾기 기능**을 통해 경로를 안내하며, 시각장애인을 고려한 **접근성 중심 UI**와 **TTS(Text-to-Speech) 기반 음성 피드백** 시스템을 통해 화면을 보지 않아도 모든 정보를 전달받을 수 있도록 설계되었습니다.

## 🔎 현장 조사 및 사용자 피드백

### 성북시각장애인복지관 방문 및 보행 체험

<div align="center">
  <table>
    <tr>
      <td><img src="docs/assets/images/성북시각장애인복지관1.png" alt="성북시각장애인복지관 방문1" width="100%"></td>
      <td><img src="docs/assets/images/성북시각장애인복지관2.png" alt="성북시각장애인복지관 방문2" width="100%"></td>
    </tr>
  </table>
</div>

- 시각장애인 이해 교육 참여
- 흰지팡이를 활용한 실외 보행 체험 (안대 착용, 점자블록 이동)
- 방향 감각 상실, 점자블록 단절 구간의 어려움 직접 경험

### 서울시립노원시각장애인복지관 방문 및 기능 검증

<div align="center">
  <img src="docs/assets/images/서울시립노원시각장애인복지관.png" alt="서울시립노원시각장애인복지관 방문" width="100%">
</div>

- 저시력 시각장애인 사용자 및 복지관 직원 대상 앱 시연
- 신호등·횡단보도 구분 안내, 장애물 거리 정보, 공중 장애물 탐지 등 개선 방향 도출
- TalkBack 연동 및 사용자 맞춤 접근성 설정 필요성 확인

## ✨ 주요 기능

### 1. 🚧 장애물 탐지 및 회피 방향 안내

- 스마트폰 카메라 영상을 기반으로 보행 중 위험한 장애물을 **실시간 탐지**
- **YOLOv11s** 모델로 사람, 차량, 자전거, 킥보드 등 객체를 인식하고, **YOLOv8-SEG**로 보행 가능 영역을 분석하여 실제 위험 여부 판단
- 탐지된 장애물의 위치·거리·위험도를 분석해 **안전한 회피 방향** 안내

### 2. 🗺️ 길찾기

- 사용자의 현재 위치와 목적지를 기반으로 **보행 경로** 안내
- **TMAP 보행자 경로 API** 활용 — 이동 거리, 소요 시간, 회전 지점 등 핵심 정보 제공
- 공공 데이터를 수집해 보행 경로 내 **횡단보도 및 음향신호기** 존재 여부 안내

### 3. 🔊 음성 및 햅틱 안내

- 화면을 보지 않아도 정보를 인지할 수 있도록 **음성(TTS)** 과 **진동(햅틱)** 으로 안내
- 위험 상황, 방향 안내, 길찾기 정보 등을 **멀티모달** 방식으로 전달
- **사용자 맞춤 설정** 지원 (음성 속도·안내 문구 길이 등 직접 조절 가능)

### 4. ♿ 접근성 중심 사용자 인터페이스

- 시각장애인의 사용성을 고려한 **간단하고 직관적인 모바일 UI**
- 큰 버튼과 최소 조작 구조를 적용해 **누구나 쉽게 사용**할 수 있도록 설계

## 🏗️ 시스템 구조도
<div align="center">
  <img src="docs/assets/images/architecture.png" alt="시스템 구조도" width="100%">
</div>

## 🛠️ 기술 스택

### Frontend

![Flutter](https://img.shields.io/badge/Flutter-02569B?style=for-the-badge&logo=flutter&logoColor=white)
![Dart](https://img.shields.io/badge/Dart-0175C2?style=for-the-badge&logo=dart&logoColor=white)

### Backend

![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![REST API](https://img.shields.io/badge/REST_API-FF6C37?style=for-the-badge&logo=postman&logoColor=white)
![NGINX](https://img.shields.io/badge/NGINX-009639?style=for-the-badge&logo=nginx&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

### AI / ML

![FastAPI](https://img.shields.io/badge/FastAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white)
![PyTorch](https://img.shields.io/badge/PyTorch-EE4C2C?style=for-the-badge&logo=pytorch&logoColor=white)
![YOLOv11s](https://img.shields.io/badge/YOLOv11s-00FFFF?style=for-the-badge&logo=yolo&logoColor=black)
![YOLO--SEG](https://img.shields.io/badge/YOLO--SEG-00CFFF?style=for-the-badge&logo=yolo&logoColor=black)
![ByteTrack](https://img.shields.io/badge/ByteTrack-FF4500?style=for-the-badge&logoColor=white)
![Depth--Anything](https://img.shields.io/badge/Depth--Anything-8A2BE2?style=for-the-badge&logoColor=white)

### External API

![TMAP](https://img.shields.io/badge/TMAP_API-FF6D00?style=for-the-badge&logo=map&logoColor=white)
![Kakao](https://img.shields.io/badge/Kakao_Local_API-FFCD00?style=for-the-badge&logo=kakao&logoColor=black)
![OpenWeather](https://img.shields.io/badge/OpenWeather_API-EB6E4B?style=for-the-badge&logo=openweathermap&logoColor=white)
![공공데이터](https://img.shields.io/badge/공공데이터_포털-0066CC?style=for-the-badge&logo=databricks&logoColor=white)
![서울시횡단보도](https://img.shields.io/badge/서울시_횡단보도_API-005BAC?style=for-the-badge&logoColor=white)
![서울시음향신호기](https://img.shields.io/badge/서울시_음향신호기_API-007BFF?style=for-the-badge&logoColor=white)

### Data

![Roboflow](https://img.shields.io/badge/Roboflow-7B2FF7?style=for-the-badge&logo=roboflow&logoColor=white)
![AIHub](https://img.shields.io/badge/AIHub_인도보행영상-00A86B?style=for-the-badge&logoColor=white)

### Collaboration

![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)
![Notion](https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white)
![Figma](https://img.shields.io/badge/Figma-F24E1E?style=for-the-badge&logo=figma&logoColor=white)

## 📖 사용법

<div align="center">
  <table>
    <tr>
      <td align="center" width="33%">
        <img src="docs/assets/images/testguide1.png" alt="시작 화면 & 홈 화면" width="100%">
      </td>
      <td align="center" width="33%">
        <img src="docs/assets/images/testguide2.png" alt="장애물 탐지 화면" width="100%">
      </td>
      <td align="center" width="33%">
        <img src="docs/assets/images/testguide3.png" alt="길찾기 화면" width="100%">
      </td>
    </tr>
    <tr>
      <td align="center" width="33%">
        <img src="docs/assets/images/testguide4.png" alt="저장된 장소 관리" width="100%">
      </td>
      <td align="center" width="33%">
        <img src="docs/assets/images/testguide5.png" alt="길찾기 중 & 설정 화면" width="100%">
      </td>
      <td align="center" width="33%">
        <img src="docs/assets/images/testguide6.png" alt="설정 & 계정 화면" width="100%">
      </td>
    </tr>
  </table>
</div>

1. **시작 화면 / 홈 화면** — 카카오 로그인 후 장애물 탐지, 길찾기, 설정 기능에 접근합니다.
2. **장애물 탐지 화면** — 탐지 시작 버튼을 누르면 카메라가 실시간으로 장애물을 감지하고 진동으로 알려줍니다.
3. **길찾기 화면** — 목적지를 키보드 또는 음성으로 입력하고 안내를 시작합니다.
4. **저장된 장소 관리** — 자주 가는 장소를 저장하고, 카테고리 아이콘으로 구분하여 빠르게 선택합니다.
5. **길찾기 중 / 설정 화면** — 남은 거리·소요 시간·위험 요소를 안내하고, 안내 문구 길이와 진동 강도를 조절합니다.
6. **설정 / 계정 화면** — 알림·소리 설정, 앱 정보 확인, 로그아웃 및 회원 탈퇴를 할 수 있습니다.

## 📁 폴더 구조

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

## 💡 기대 효과

| 분야 | 기대 효과 |
| ------ | ----------- |
| 🦺 **안전성** | 실시간 장애물 탐지로 보행 중 사고 위험 감소 |
| 🚶 **독립성** | 보조인 없이 스스로 목적지까지 이동 가능 |
| ♿ **접근성** | 시각장애인 맞춤 UI/UX로 누구나 쉽게 사용 |
| 🌍 **사회적 포용** | 이동 취약계층의 사회 참여 기회 확대 |
| 📡 **확장 가능성** | 공공 인프라 데이터 연동으로 서비스 고도화 가능 |

---

## 👥 팀16-리트리버 소개

**팀 16 — 리트리버**

| 이름 | 학번 | 역할 |
| --- | --- | --- |
| 한여진 | 20233119 | Frontend |
| 황연주 | 20233123 | Frontend |
| 전예찬 | 20213078 | Backend |
| 양나래 | 20223103 | Backend |
| 이일환 | 20213048 | AI |
| 김예지 | 20223058 | AI |

---

## 📚 참고 자료
### 접근성 및 사용자 경험
- [Web Content Accessibility Guidelines (WCAG) 2.1](https://www.w3.org/TR/WCAG21/)
- [Accessible Typography Guidelines for Inclusive Mobile App Design](https://moldstud.com/articles/p-inclusive-mobile-app-typography-guidelines-crafting-accessible-designs-for-all-users)
- [The design of auditory user interfaces for blind users](https://dl.acm.org/doi/10.1145/572020.572038)

### 지도·위치·외부 API
- [TMAP 보행자 API 공식 문서](https://tmapapi.tmapmobility.com/)
- [SK Open API 보행자 경로 안내](https://openapi.sk.com/products/detail?linkMenuSeq=45)
- [Kakao Developers Local API 문서](https://developers.kakao.com/docs/ko/local/dev-guide)
- [Naver Maps Geocoding API 문서](https://api.ncloud-docs.com/docs/ai-naver-mapsgeocoding-geocode)

### AI 모델 및 연구 논문
- [Ultralytics YOLO 문서](https://docs.ultralytics.com/)
- [VisionGPT: LLM-Assisted Real-Time Anomaly Detection for Visual Navigation](https://arxiv.org/abs/2403.12415)
- [SegFormer: Simple and Efficient Design for Semantic Segmentation with Transformers](https://arxiv.org/abs/2105.15203)
- [Hugging Face SegFormer 문서](https://huggingface.co/docs/transformers/en/model_doc/segformer)
- [Depth Anything V2 GitHub](https://github.com/DepthAnything/Depth-Anything-V2)
- [Depth Anything V2](https://arxiv.org/abs/2406.09414)

### 데이터 및 공공 자료
- [공공데이터포털](https://www.data.go.kr/)
- [서울 열린데이터광장](https://data.seoul.go.kr/)
- [AIHub 인도 보행 영상 데이터셋](https://aihub.or.kr/aidata/136)
- [Roboflow Public Datasets](https://public.roboflow.com/)

### 기획 및 검증 기반

- 성북시각장애인복지관 방문 및 흰지팡이 보행 체험  
  시각장애인 보행 환경 이해, 점자블록 단절 및 방향 감각 상실 문제 확인

- 서울시립노원시각장애인복지관 사용자 테스트 및 인터뷰  
  저시력 시각장애인 사용자 대상 앱 시연, 접근성 및 보행 안전 기능 피드백 수집

---

<div align="center">
© 2026 Team 리트리버 | 길벗 프로젝트

*"당신의 걸음 곁에, 길벗"*

</div>
