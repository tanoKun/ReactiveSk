package com.github.tanokun.addon.intermediate.reduce;

import net.bytebuddy.asm.Advice;

public class TestAdvice {
    @Advice.OnMethodEnter()
    public static void enter() {
        System.out.println("TestAdvice enter.");
    }
}
