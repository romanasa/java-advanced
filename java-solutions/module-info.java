module ru.ifmo.rain.korobkov {
    requires info.kgeorgiy.java.advanced.crawler;
    requires info.kgeorgiy.java.advanced.hello;
    requires info.kgeorgiy.java.advanced.arrayset;
    requires info.kgeorgiy.java.advanced.base;
    requires info.kgeorgiy.java.advanced.concurrent;
    requires info.kgeorgiy.java.advanced.mapper;
    requires info.kgeorgiy.java.advanced.student;
    requires info.kgeorgiy.java.advanced.walk;
    requires java.rmi;
    requires org.junit.jupiter.api;
    requires org.junit.platform.engine;
    requires org.junit.platform.launcher;
    requires hamcrest.core;

    exports ru.ifmo.rain.korobkov.bank to java.rmi;
}