/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.inject.persist.jpa;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.orm.AmbariJpaLocalTxnInterceptor;
import org.apache.ambari.server.orm.AmbariLocalSessionInterceptor;
import org.apache.ambari.server.orm.PersistenceType;
import org.apache.ambari.server.orm.RequiresSession;

import com.google.common.collect.Lists;
import com.google.inject.Provides;
import com.google.inject.persist.PersistModule;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;
import com.google.inject.persist.finder.DynamicFinder;
import com.google.inject.persist.finder.Finder;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;
import static org.eclipse.persistence.config.PersistenceUnitProperties.CREATE_JDBC_DDL_FILE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.CREATE_ONLY;
import static org.eclipse.persistence.config.PersistenceUnitProperties.CREATE_OR_EXTEND;
import static org.eclipse.persistence.config.PersistenceUnitProperties.DDL_BOTH_GENERATION;
import static org.eclipse.persistence.config.PersistenceUnitProperties.DDL_GENERATION;
import static org.eclipse.persistence.config.PersistenceUnitProperties.DDL_GENERATION_MODE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.DROP_AND_CREATE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.DROP_JDBC_DDL_FILE;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;

/**
 * Copy of guice persist module for local modifications
 */
public class AmbariJpaPersistModule extends PersistModule {
  private final String jpaUnit;

  public AmbariJpaPersistModule(String jpaUnit) {
    if (null != jpaUnit && jpaUnit.length() > 0) {
      this.jpaUnit = jpaUnit;
    } else {
      throw new IllegalArgumentException("jpaUnit should not be null");
    }
  }


  private MethodInterceptor transactionInterceptor;

  @Override
  protected void configurePersistence() {
    bindConstant().annotatedWith(Jpa.class).to(jpaUnit);


    bind(AmbariJpaPersistService.class).in(Singleton.class);

    bind(PersistService.class).to(AmbariJpaPersistService.class);
    bind(UnitOfWork.class).to(AmbariJpaPersistService.class);
    bind(EntityManager.class).toProvider(AmbariJpaPersistService.class);
    bind(EntityManagerFactory.class).toProvider(JpaPersistService.EntityManagerFactoryProvider.class);

    transactionInterceptor = new AmbariJpaLocalTxnInterceptor();
    requestInjection(transactionInterceptor);

    MethodInterceptor sessionInterceptor = new AmbariLocalSessionInterceptor();
    requestInjection(sessionInterceptor);

    // Bind dynamic finders.
    for (Class<?> finder : dynamicFinders) {
      bindFinder(finder);
    }

    bindInterceptor(annotatedWith(RequiresSession.class), any(), sessionInterceptor);
    bindInterceptor(any(), annotatedWith(RequiresSession.class), sessionInterceptor);
  }


  @Override
  protected MethodInterceptor getTransactionInterceptor() {
    return transactionInterceptor;
  }

  /**
   * Configures the JPA persistence provider with a set of properties.
   *
   * @param properties A set of name value pairs that configure a JPA persistence
   *                   provider as per the specification.
   */
  public AmbariJpaPersistModule properties(Properties properties) {

    return this;
  }

  private final List<Class<?>> dynamicFinders = Lists.newArrayList();

  /**
   * Adds an interface to this module to use as a dynamic finder.
   *
   * @param iface Any interface type whose methods are all dynamic finders.
   */
  public <T> AmbariJpaPersistModule addFinder(Class<T> iface) {
    dynamicFinders.add(iface);
    return this;
  }

  private <T> void bindFinder(Class<T> iface) {
    if (!isDynamicFinderValid(iface)) {
      return;
    }

    InvocationHandler finderInvoker = new InvocationHandler() {
      @Inject
      JpaFinderProxy finderProxy;

      @Override
      public Object invoke(final Object thisObject, final Method method, final Object[] args)
          throws Throwable {

        // Don't intercept non-finder methods like equals and hashcode.
        if (!method.isAnnotationPresent(Finder.class)) {
          // This is not ideal, we are using the invocation handler's equals
          // and hashcode as a proxy (!) for the proxy's equals and hashcode.
          return method.invoke(this, args);
        }

        return finderProxy.invoke(new MethodInvocation() {
          @Override
          public Method getMethod() {
            return method;
          }

          @Override
          public Object[] getArguments() {
            return null == args ? new Object[0] : args;
          }

          @Override
          public Object proceed() throws Throwable {
            return method.invoke(thisObject, args);
          }

          @Override
          public Object getThis() {
            throw new UnsupportedOperationException("Bottomless proxies don't expose a this.");
          }

          @Override
          public AccessibleObject getStaticPart() {
            throw new UnsupportedOperationException();
          }
        });
      }
    };
    requestInjection(finderInvoker);

    @SuppressWarnings("unchecked") // Proxy must produce instance of type given.
        T proxy = (T) Proxy
        .newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] { iface },
            finderInvoker);

    bind(iface).toInstance(proxy);
  }

  private boolean isDynamicFinderValid(Class<?> iface) {
    boolean valid = true;
    if (!iface.isInterface()) {
      addError(iface + " is not an interface. Dynamic Finders must be interfaces.");
      valid = false;
    }

    for (Method method : iface.getMethods()) {
      DynamicFinder finder = DynamicFinder.from(method);
      if (null == finder) {
        addError("Dynamic Finder methods must be annotated with @Finder, but " + iface
            + "." + method.getName() + " was not");
        valid = false;
      }
    }
    return valid;
  }

  /**
   * Provides a Properties instance with all the JPA related configuration from the configuration.
   * As a result of this provider method, whenever a Properties instance is requested for injection, that is annotated
   * with the @Jpa annotation an instance setup by this method will be injected.
   *
   * The @Singleton annotation ensures the instance will be a singleton across the application.
   *
   * @param configuration the application configuration (expected to be bound out of this module)
   * @return a Properties instance with the JPA configuration
   */
  @Provides
  @Jpa
  @Singleton
  private Properties jpaProperties(Configuration configuration) {

    // todo move the logic from the static method here!
    Properties persistenceProperties = ControllerModule.getPersistenceProperties(configuration);

    if (!configuration.getPersistenceType().equals(PersistenceType.IN_MEMORY)) {
      persistenceProperties.setProperty(JDBC_USER, configuration.getDatabaseUser());
      persistenceProperties.setProperty(JDBC_PASSWORD, configuration.getDatabasePassword());

      switch (configuration.getJPATableGenerationStrategy()) {
        case CREATE:
          persistenceProperties.setProperty(DDL_GENERATION, CREATE_ONLY);
          //dbInitNeeded = true;
          break;
        case DROP_AND_CREATE:
          persistenceProperties.setProperty(DDL_GENERATION, DROP_AND_CREATE);
          //dbInitNeeded = true;
          break;
        case CREATE_OR_EXTEND:
          persistenceProperties.setProperty(DDL_GENERATION, CREATE_OR_EXTEND);
          break;
        default:
          break;
      }

      persistenceProperties.setProperty(DDL_GENERATION_MODE, DDL_BOTH_GENERATION);
      persistenceProperties.setProperty(CREATE_JDBC_DDL_FILE, "DDL-create.jdbc");
      persistenceProperties.setProperty(DROP_JDBC_DDL_FILE, "DDL-drop.jdbc");
    }

    return persistenceProperties;
  }
}
