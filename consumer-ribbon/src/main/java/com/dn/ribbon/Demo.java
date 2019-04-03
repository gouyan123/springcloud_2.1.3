package com.dn.ribbon;

import java.util.concurrent.*;

public class Demo {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Future<String> future = executorService.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return demo();
            }
        });
        String resutlt = future.get();
        System.out.println(resutlt);
        executorService.shutdown();
    }
    public static String demo(){
        return "demo";
    }
}
