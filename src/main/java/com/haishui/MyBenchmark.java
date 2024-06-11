package com.haishui;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.ObjIntConsumer;

@State(Scope.Thread)
@Fork(1) // Fork 1个进程进行测试
@BenchmarkMode(Mode.Throughput) // 平均时间
@Warmup(iterations = 4) // JIT预热
@Measurement(iterations = 4, time = 5) // 迭代5次,每次10s
@OutputTimeUnit(TimeUnit.MILLISECONDS) // 结果所使用的时间单位
@Threads(2) // 线程4个
public class MyBenchmark {
    private static final String STR_FIELD_NAME = "strValue";
    private static final String INT_FIELD_NAME = "intValue";
    private static final String STR_SETTER_NAME = "setStrValue";
    private static final String INT_SETTER_NAME = "setIntValue";
    private static final String STR = "Hello World!";
    private static final int INT = 10000;
    private TestData testData;

    @Setup(Level.Iteration)
    public void setup() {
        testData = new TestData();
    }


    @Benchmark
    public void testDirectlyCall(Blackhole blackhole) {
        testData.setStrValue(STR);
        testData.setIntValue(INT);
        blackhole.consume(testData);
    }

    @Benchmark
    public void testNonStaticField(Blackhole blackhole) throws NoSuchFieldException, IllegalAccessException {
        Field strField = TestData.class.getDeclaredField(STR_FIELD_NAME);
        strField.setAccessible(true);
        Field intField = TestData.class.getDeclaredField(INT_FIELD_NAME);
        intField.setAccessible(true);
        strField.set(testData, STR);
        intField.setInt(testData, INT);
        blackhole.consume(testData);
    }

    private static final Field STR_FIELD;
    private static final Field INT_FIELD;

    static {
        try {
            STR_FIELD = TestData.class.getDeclaredField(STR_FIELD_NAME);
            STR_FIELD.setAccessible(true);
            INT_FIELD = TestData.class.getDeclaredField(INT_FIELD_NAME);
            INT_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    public void testStaticField(Blackhole blackhole) throws IllegalAccessException {
        STR_FIELD.set(testData, STR);
        INT_FIELD.setInt(testData, INT);
        blackhole.consume(testData);
    }

    @Benchmark
    public void testNonStaticMethod(Blackhole blackhole) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method strSetter = TestData.class.getDeclaredMethod(STR_SETTER_NAME, String.class);
        Method intSetter = TestData.class.getDeclaredMethod(INT_SETTER_NAME, int.class);
        strSetter.invoke(testData, STR);
        intSetter.invoke(testData, INT);
        blackhole.consume(testData);
    }

    private static final Method STR_SETTER;
    private static final Method INT_SETTER;

    static {
        try {
            STR_SETTER = TestData.class.getDeclaredMethod(STR_SETTER_NAME, String.class);
            INT_SETTER = TestData.class.getDeclaredMethod(INT_SETTER_NAME, int.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    public void testStaticMethod(Blackhole blackhole) throws InvocationTargetException, IllegalAccessException {
        STR_SETTER.invoke(testData, STR);
        INT_SETTER.invoke(testData, INT);
        blackhole.consume(testData);
    }

    @Benchmark
    public void testNonStaticVarHandle(Blackhole blackhole) throws IllegalAccessException, NoSuchFieldException {
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(TestData.class, MethodHandles.lookup());
        VarHandle strVarHandle = lookup.findVarHandle(TestData.class, STR_FIELD_NAME, String.class);
        strVarHandle.accessModeType(VarHandle.AccessMode.SET);
        VarHandle intVarHandle = lookup.findVarHandle(TestData.class, INT_FIELD_NAME, int.class);
        intVarHandle.accessModeType(VarHandle.AccessMode.SET);
        strVarHandle.set(testData, STR);
        intVarHandle.set(testData, INT);
        blackhole.consume(testData);
    }

    private static final VarHandle STR_VAR_HANDLE;
    private static final VarHandle INT_VAR_HANDLE;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(TestData.class, MethodHandles.lookup());
            STR_VAR_HANDLE = lookup.findVarHandle(TestData.class, STR_FIELD_NAME, String.class);
            STR_VAR_HANDLE.accessModeType(VarHandle.AccessMode.SET);
            INT_VAR_HANDLE = lookup.findVarHandle(TestData.class, INT_FIELD_NAME, int.class);
            INT_VAR_HANDLE.accessModeType(VarHandle.AccessMode.SET);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    public void testStaticVarHandle(Blackhole blackhole) {
        STR_VAR_HANDLE.set(testData, STR);
        INT_VAR_HANDLE.set(testData, INT);
        blackhole.consume(testData);
    }

    @Benchmark
    public void testNonStaticFieldMethodHandle(Blackhole blackhole) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(TestData.class, MethodHandles.lookup());
        MethodHandle strMethodHandle = lookup.findSetter(TestData.class, STR_FIELD_NAME, String.class);
        MethodHandle intMethodHandle = lookup.findSetter(TestData.class, INT_FIELD_NAME, int.class);
        strMethodHandle.invoke(testData, STR);
        intMethodHandle.invoke(testData, INT);
        blackhole.consume(testData);
    }

    private static final MethodHandle STR_FIELD_METHOD_HANDLE;
    private static final MethodHandle INT_FIELD_METHOD_HANDLE;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(TestData.class, MethodHandles.lookup());
            STR_FIELD_METHOD_HANDLE = lookup.findSetter(TestData.class, STR_FIELD_NAME, String.class);
            INT_FIELD_METHOD_HANDLE = lookup.findSetter(TestData.class, INT_FIELD_NAME, int.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    public void testStaticFieldMethodHandle(Blackhole blackhole) throws Throwable {
        STR_FIELD_METHOD_HANDLE.invokeExact(testData, STR);
        INT_FIELD_METHOD_HANDLE.invokeExact(testData, INT);
        blackhole.consume(testData);
    }

    private static final MethodHandle STR_SETTER_METHOD_HANDLE;
    private static final MethodHandle INT_SETTER_METHOD_HANDLE;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodType strMethodType = MethodType.methodType(void.class, String.class);
            STR_SETTER_METHOD_HANDLE = lookup.findVirtual(TestData.class, STR_SETTER_NAME, strMethodType);
            MethodType intMethodType = MethodType.methodType(void.class, int.class);
            INT_SETTER_METHOD_HANDLE = lookup.findVirtual(TestData.class, INT_SETTER_NAME, intMethodType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    public void testStaticSetterMethodHandle(Blackhole blackhole) throws Throwable {
        STR_SETTER_METHOD_HANDLE.invokeExact(testData, STR);
        INT_SETTER_METHOD_HANDLE.invokeExact(testData, INT);
        blackhole.consume(testData);
    }

    private static final BiConsumer<TestData, String> SET_STR_FUNC;
    private static final ObjIntConsumer<TestData> SET_INT_FUNC;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle strSetterMH = lookup.unreflect(STR_SETTER);
            MethodHandle intSetterMH = lookup.unreflect(INT_SETTER);
            CallSite strCallSite = LambdaMetafactory.metafactory(
                    lookup,
                    "accept",
                    MethodType.methodType(BiConsumer.class),
                    MethodType.methodType(void.class, Object.class, Object.class),
                    strSetterMH,
                    strSetterMH.type());
            CallSite intCallSite = LambdaMetafactory.metafactory(
                    lookup,
                    "accept",
                    MethodType.methodType(ObjIntConsumer.class),
                    MethodType.methodType(void.class, Object.class, int.class),
                    intSetterMH,
                    intSetterMH.type());
            // noinspection unchecked
            SET_STR_FUNC = (BiConsumer<TestData, String>) strCallSite.getTarget().invokeExact();
            // noinspection unchecked
            SET_INT_FUNC = (ObjIntConsumer<TestData>) intCallSite.getTarget().invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    public void testStaticLambda(Blackhole blackhole) {
        SET_STR_FUNC.accept(testData, STR);
        SET_INT_FUNC.accept(testData, INT);
        blackhole.consume(testData);
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(MyBenchmark.class.getSimpleName())
                .result("./reflect-benchmark-result.json")
                .resultFormat(ResultFormatType.JSON)
                .build();
        new Runner(options).run();
    }
}
