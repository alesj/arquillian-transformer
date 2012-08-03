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
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapeDwarfJUnitTransformer extends JavassistTransformer {
    protected static final Random RANDOM = new Random();

    protected void transform(CtClass clazz) throws Exception {
        CtClass current = clazz;
        while (current != null) {
            for (CtMethod m : current.getMethods()) {
                if (isDeploymentMethod(m) && shouldAddGaeLib(current)) {
                    addGaeApiLib(current, m);
                }
            }
            current = current.getSuperclass();
        }
    }

    protected boolean isDeploymentMethod(CtMethod m) {
        return ((m.getModifiers() & Modifier.STATIC) == Modifier.STATIC) && m.hasAnnotation(Deployment.class);
    }

    protected boolean shouldAddGaeLib(CtClass clazz) {
        // testsuite tests already have appengine api lib
        return clazz.getName().contains(".testsuite.") == false;
    }

    protected void addGaeApiLib(CtClass clazz, CtMethod m) throws Exception {
        ClassPool pool = clazz.getClassPool();
        ClassFile ccFile = clazz.getClassFile();
        ConstPool constPool = ccFile.getConstPool();

        // remove @Deployment annotation
        AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        attr.setAnnotations(new Annotation[0]);
        m.getMethodInfo().addAttribute(attr);

        // create new method with @Deployment
        CtClass archiveClass = pool.get(WebArchive.class.getName());
        CtMethod newDeployment = new CtMethod(archiveClass, "getDeployment_" + Math.abs(RANDOM.nextInt()), new CtClass[]{}, clazz);
        newDeployment.setModifiers(Modifier.STATIC | Modifier.PUBLIC);
        newDeployment.setBody(getDeploymentMethodBody(m));
        String deploymentClassName = Deployment.class.getName();
        constPool.addUtf8Info(deploymentClassName);
        attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        Annotation annotation = new Annotation(deploymentClassName, constPool);
        attr.addAnnotation(annotation);
        newDeployment.getMethodInfo().addAttribute(attr);
        clazz.addMethod(newDeployment);
    }

    protected String getDeploymentMethodBody(CtMethod original) {
        return "{" +
                "org.jboss.shrinkwrap.api.spec.WebArchive war = (org.jboss.shrinkwrap.api.spec.WebArchive) " + original.getName() + "();" +
                "org.jboss.maven.arquillian.transformer.CapeDwarfJUnitTransformer.addGaeApiLib(war);" +
                "return war;" +
                "}";
    }

    public static void addGaeApiLib(WebArchive war) {
        MavenDependencyResolver resolver = DependencyResolvers.use(MavenDependencyResolver.class).loadMetadataFromPom("pom.xml");
        war.addAsLibraries(resolver.artifact("com.google.appengine:appengine-api-1.0-sdk").resolveAsFiles());
    }
}
