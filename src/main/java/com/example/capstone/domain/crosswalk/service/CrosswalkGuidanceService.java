package com.example.capstone.domain.crosswalk.service;

import com.example.capstone.domain.crosswalk.entity.Crosswalk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CrosswalkGuidanceService {

    public List<String> generateStaticGuidance(Crosswalk c) {
        List<String> messages = new ArrayList<>();

        messages.add("전방에 횡단보도가 있습니다.");

        if ("Y".equalsIgnoreCase(c.getTfclghtYn())) {
            messages.add("보행자 신호등이 있는 횡단보도입니다.");
        } else {
            messages.add("보행자 신호등이 없는 횡단보도일 수 있습니다.");
        }

        if ("Y".equalsIgnoreCase(c.getSondSgngnrYn())) {
            messages.add("음향신호기가 설치된 횡단보도입니다.");
        }

        if ("Y".equalsIgnoreCase(c.getFnctngSgngnrYn())) {
            messages.add("보행자 버튼 신호기가 있는 횡단보도일 수 있습니다.");
        }

        if ("Y".equalsIgnoreCase(c.getBrllBlckYn())) {
            messages.add("점자블록이 있는 횡단보도 구간입니다.");
        }

        if ("Y".equalsIgnoreCase(c.getFtpthLowerYn())) {
            messages.add("보도 턱이 낮아진 구간입니다.");
        }

        return messages;
    }
}