/*
 * Copyright 2011-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.cassandra.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.mapping.Table;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

/**
 * Base class for Spring Data Cassandra configuration using JavaConfig.
 * 
 * @author Alex Shvid
 */
@Configuration
public abstract class AbstractCassandraConfiguration {

	/**
	 * Return the name of the keyspace to connect to.
	 * 
	 * @return must not be {@literal null}.
	 */
	protected abstract String getKeyspaceName();
	
	/**
	 * Return the {@link Cluster} instance to connect to.
	 * 
	 * @return
	 * @throws Exception
	 */
	@Bean
	public abstract Cluster cluster() throws Exception;
	
	/**
	 * Creates a {@link Session} to be used by the {@link CassandraTemplate}. Will use the {@link Cluster} instance
	 * configured in {@link #cluster()}.
	 * 
	 * @see #cluster()
	 * @see #cassandraTemplate()
	 * @return
	 * @throws Exception
	 */
	@Bean
	public Session keyspace() throws Exception {
		String keyspace = getKeyspaceName();
		if (StringUtils.hasText(keyspace)) {
			return cluster().connect(keyspace);
		}
		else {
			return cluster().connect();
		}
	}
	
	/**
	 * Return the base package to scan for mapped {@link Table}s. Will return the package name of the configuration
	 * class' (the concrete class, not this one here) by default. So if you have a {@code com.acme.AppConfig} extending
	 * {@link AbstractCassandraConfiguration} the base package will be considered {@code com.acme} unless the method is
	 * overriden to implement alternate behaviour.
	 * 
	 * @return the base package to scan for mapped {@link Table} classes or {@literal null} to not enable scanning for
	 *         entities.
	 */
	protected String getMappingBasePackage() {
		return getClass().getPackage().getName();
	}
	
	/**
	 * Creates a {@link CassandraTemplate}.
	 * 
	 * @return
	 * @throws Exception
	 */
	@Bean
	public CassandraTemplate cassandraTemplate() throws Exception {
		return new CassandraTemplate(keyspace());
	}
	
	/**
	 * Scans the mapping base package for classes annotated with {@link Table}.
	 * 
	 * @see #getMappingBasePackage()
	 * @return
	 * @throws ClassNotFoundException
	 */
	protected Set<Class<?>> getInitialEntitySet() throws ClassNotFoundException {

		String basePackage = getMappingBasePackage();
		Set<Class<?>> initialEntitySet = new HashSet<Class<?>>();

		if (StringUtils.hasText(basePackage)) {
			ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(
					false);
			componentProvider.addIncludeFilter(new AnnotationTypeFilter(Table.class));
			componentProvider.addIncludeFilter(new AnnotationTypeFilter(Persistent.class));

			for (BeanDefinition candidate : componentProvider.findCandidateComponents(basePackage)) {
				initialEntitySet.add(ClassUtils.forName(candidate.getBeanClassName(),
						AbstractCassandraConfiguration.class.getClassLoader()));
			}
		}

		return initialEntitySet;
	}
	
}