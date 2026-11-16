package com.example.tickers;

/** Made up: makes each ticker once, in a JVM asked to write lambdas' classes, and prints their classes' names. */
public final class Dumper {
    private Dumper() {}

    public static void main(String[] args) {
        System.out.println("one " + OneTicker.ticker(true).getClass().getName());
        System.out.println("two-server " + TwoTickers.ticker(true).getClass().getName());
        System.out.println("two-client " + TwoTickers.ticker(false).getClass().getName());
        System.out.println("capturing " + Capturing.ticker(3).getClass().getName());
    }
}
