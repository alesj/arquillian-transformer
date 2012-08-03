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

import javassist.CtClass;
import javassist.CtMethod;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapeDwarfJUnitTransformer extends JavassistTransformer {
    protected void transform(CtClass clazz) throws Exception {
        CtClass current = clazz;
        while (current != null) {
            for (CtMethod m : current.getDeclaredMethods()) {
                if (isDeploymentMethod(m) && shouldAddGaeLib(current, m)) {
                    addGaeApiLib(m);
                }
            }
            current = current.getSuperclass();
        }
    }

    protected boolean isDeploymentMethod(CtMethod m) {
        return (m.getModifiers() & Modifier.STATIC) == Modifier.STATIC && m.hasAnnotation(Deployment.class);
    }

    protected boolean shouldAddGaeLib(CtClass clazz, CtMethod m) {
        // testsuite tests already have appengine api lib
        return clazz.getName().contains(".testsuite.") == false;
    }

    protected void addGaeApiLib(CtMethod m) throws Exception {
        // TODO
    }

    public static void addGaeApiLib(WebArchive war) {
        MavenDependencyResolver resolver = DependencyResolvers.use(MavenDependencyResolver.class).loadMetadataFromPom("pom.xml");
        war.addAsLibraries(resolver.artifact("com.google.appengine:appengine-api-1.0-sdk").resolveAsFiles());
    }
}
