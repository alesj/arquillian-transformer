/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.maven.arquillian.transformer;

import java.lang.reflect.Modifier;
import java.util.Random;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ClassMemberValue;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class ArquillianJUnitTransformer extends JavassistTransformer {
    protected void transform(CtClass clazz) throws Exception {
        addRunWithArquillian(clazz);
        addDeploymentMethod(clazz);
        addTestAnnotations(clazz);
        addLifecycleMethods(clazz);
    }

    protected void addRunWithArquillian(CtClass clazz) throws Exception {
        ClassFile ccFile = clazz.getClassFile();
        ConstPool constPool = ccFile.getConstPool();
        AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);

        String runWithClassName = RunWith.class.getName();
        String arquillianClassName = Arquillian.class.getName();

        constPool.addUtf8Info(runWithClassName);
        constPool.addUtf8Info(arquillianClassName);

        Annotation annotation = new Annotation(runWithClassName, constPool);
        annotation.addMemberValue("value", new ClassMemberValue(arquillianClassName, constPool));
        attr.addAnnotation(annotation);

        ccFile.addAttribute(attr);
    }

    protected void addDeploymentMethod(CtClass clazz) throws Exception {
        ClassPool pool = clazz.getClassPool();
        CtClass archiveClass = pool.get(WebArchive.class.getName());

        CtMethod m = new CtMethod(archiveClass, "getDeployment", new CtClass[]{}, clazz);
        m.setModifiers(Modifier.STATIC | Modifier.PUBLIC);
        addDeploymentAnnotation(clazz, m);
        m.setBody(getDeploymentMethodBody(clazz));

        clazz.addMethod(m);
    }

    protected abstract String getDeploymentMethodBody(CtClass clazz) throws Exception;

    protected void addTestAnnotations(CtClass clazz) throws Exception {
        for (CtMethod m : clazz.getMethods()) {
            if (isTestMethod(m)) {
                addTestAnnotation(clazz, m);
            }
        }
    }

    protected boolean isTestMethod(CtMethod m) throws Exception {
        return m.getName().startsWith("test") && m.getParameterTypes().length == 0 && ((m.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC);
    }

    protected void addDeploymentAnnotation(CtClass clazz, CtMethod method) throws Exception {
        ClassFile ccFile = clazz.getClassFile();
        ConstPool constPool = ccFile.getConstPool();
        AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);

        String deploymentClassName = Deployment.class.getName();

        constPool.addUtf8Info(deploymentClassName);

        Annotation annotation = new Annotation(deploymentClassName, constPool);
        attr.addAnnotation(annotation);

        method.getMethodInfo().addAttribute(attr);
    }

    protected void addTestAnnotation(CtClass clazz, CtMethod method) throws Exception {
        ClassFile ccFile = clazz.getClassFile();
        ConstPool constPool = ccFile.getConstPool();
        AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);

        String testClassName = Test.class.getName();

        constPool.addUtf8Info(testClassName);

        Annotation annotation = new Annotation(testClassName, constPool);
        attr.addAnnotation(annotation);

        method.getMethodInfo().addAttribute(attr);
    }

    protected void addLifecycleMethods(CtClass clazz) throws Exception {
        Random random = new Random();

        ClassPool pool = clazz.getClassPool();
        CtClass voidClass = pool.get(Void.TYPE.getName());
        CtClass exceptionClass = pool.get(Exception.class.getName());

        ClassFile ccFile = clazz.getClassFile();
        ConstPool constPool = ccFile.getConstPool();

        CtMethod before = new CtMethod(voidClass, "before" + random.nextInt(), new CtClass[]{}, clazz);
        before.setModifiers(Modifier.PUBLIC);
        before.setBody("{" + setUpSrc() + "}");
        before.setExceptionTypes(new CtClass[]{exceptionClass});
        AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        String beforeClassName = Before.class.getName();
        constPool.addUtf8Info(beforeClassName);
        Annotation annotation = new Annotation(beforeClassName, constPool);
        attr.addAnnotation(annotation);
        before.getMethodInfo().addAttribute(attr);
        clazz.addMethod(before);

        CtMethod after = new CtMethod(voidClass, "after" + random.nextInt(), new CtClass[]{}, clazz);
        after.setModifiers(Modifier.PUBLIC);
        after.setBody("{" + tearDownSrc() + "}");
        after.setExceptionTypes(new CtClass[]{exceptionClass});
        attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        String afterClassName = After.class.getName();
        constPool.addUtf8Info(afterClassName);
        annotation = new Annotation(afterClassName, constPool);
        attr.addAnnotation(annotation);
        after.getMethodInfo().addAttribute(attr);
        clazz.addMethod(after);
    }

    protected String setUpSrc() {
        return "setUp();";
    }

    protected String tearDownSrc() {
        return "tearDown();";
    }
}
