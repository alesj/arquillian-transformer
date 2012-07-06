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

import javassist.CtClass;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class AppEngineDataNucleusTransformer extends ArquillianJUnitTransformer {
    protected String getDeploymentMethodBody(CtClass clazz) throws Exception {
        return "{return org.jboss.maven.arquillian.transformer.AppEngineDataNucleusTransformer.buildArchive();}";
    }

    public static WebArchive buildArchive() {
        MavenDependencyResolver resolver = DependencyResolvers.use(MavenDependencyResolver.class).loadMetadataFromPom("pom.xml");
        WebArchive war = ShrinkWrap.create(WebArchive.class);
        war.setWebXML(new org.jboss.shrinkwrap.api.asset.StringAsset("<web/>"));
        war.addAsWebInfResource("appengine-web.xml");
        war.addAsWebInfResource("META-INF/persistence.xml", "classes/META-INF/persistence.xml");
        war.addAsLibraries(resolver.artifact("com.google.appengine:appengine-api-1.0-sdk").resolveAsFiles());
        war.addAsLibraries(resolver.artifact("org.datanucleus:datanucleus-core").resolveAsFiles());
        war.addAsLibraries(resolver.artifact("org.datanucleus:datanucleus-api-jdo").resolveAsFiles());
        war.addAsLibraries(resolver.artifact("org.datanucleus:datanucleus-api-jpa").resolveAsFiles());
        war.addAsLibraries(resolver.artifact("com.google.appengine.orm:datanucleus-appengine").resolveAsFiles());
        war.addAsLibraries(resolver.artifact("javax.jdo:jdo-api").resolveAsFiles());
        war.addAsLibraries(resolver.artifact("org.apache.geronimo.specs:geronimo-jta_2.0_spec").resolveAsFiles());
        return war;
    }
}
