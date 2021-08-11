package com.study.cache.redis;

import org.junit.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.io.IOException;

/**
 * spring EL 表达式测试
 */
public class SpringELTests {

    @Test
    public void test1() {
        // JAVA占位符
        String template = "%s ： 正在测试";
        String result = String.format(template, "我是Tony");
        System.out.println(result);
    }

    @Test
    public void SPELTest() throws IOException, Exception {
        String ELdemo = "'hello:' + #userId"; // EL表达式 （可以简单理解为动态的占位符）

        // 1、创建解析器
        ExpressionParser parser = new SpelExpressionParser();
        Expression expression = parser.parseExpression(ELdemo);
        // 2、设置解析上下文（有哪些占位符，以及每种占位符的值）
        EvaluationContext context = new StandardEvaluationContext(); // 参数
        context.setVariable("userId", "tony");
        // 3、解析
        String result = expression.getValue(context).toString();
        System.out.println(result);
    }
}
