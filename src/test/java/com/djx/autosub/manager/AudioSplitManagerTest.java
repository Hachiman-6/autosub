package com.djx.autosub.manager;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AudioSplitManagerTest {

    @Test
    void splitAudio() {
        AudioSplitManager audioSplitManager = new AudioSplitManager();
        // 20:05, 40:07
        List<Integer> timeList = new ArrayList<>();
        timeList.add(20 * 60 + 5);
        timeList.add(40 * 60 + 7);
        String audioFilePath = "D:\\SpringWeb_xiangmu\\test\\audio\\d2afa04d8e994a25870eda8fce4973b9.mp3";
        audioSplitManager.splitAudio(timeList, audioFilePath);
    }
}