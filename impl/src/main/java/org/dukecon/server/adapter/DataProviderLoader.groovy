package org.dukecon.server.adapter

import org.dukecon.server.conference.ConferencesConfiguration
import org.dukecon.server.conference.ConferencesConfigurationService
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.core.env.Environment

/**
 * Reads all conferences from configuration file and generates a #ConferenceDataProvider for each.
 *
 * @author Falk Sippach, falk@jug-da.de, @sippsack
 */
class DataProviderLoader implements BeanDefinitionRegistryPostProcessor {

    private final ConferencesConfiguration configuration = new ConferencesConfiguration()

    DataProviderLoader(Environment env) {
        final ConferencesConfigurationService configurationService = new ConferencesConfigurationService(env)
        configurationService.init()
        this.configuration.conferences.addAll(configurationService.conferences)
    }

    @Override
    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        configuration.conferences.each { ConferencesConfiguration.Conference config ->
            def rawDataMapper = config.rawDataMapperClass.newInstance(new RawDataResources(config.talksUri))

            BeanDefinitionBuilder builderDataExtractor = BeanDefinitionBuilder.genericBeanDefinition(config.extractorClass)
            builderDataExtractor.addConstructorArgValue(config)
            builderDataExtractor.addConstructorArgValue(rawDataMapper)
            builderDataExtractor.addConstructorArgReference('speakerImageService')
            beanDefinitionRegistry.registerBeanDefinition("${config.name} data extractor", builderDataExtractor.beanDefinition)

            if (config.backupUri) {
                createWebResourceDataProviderForConference(config, beanDefinitionRegistry)
            } else {
                createLocalResourceDataProviderForConference(config, beanDefinitionRegistry)
            }
        }
    }

    private void createLocalResourceDataProviderForConference(ConferencesConfiguration.Conference config, BeanDefinitionRegistry beanDefinitionRegistry) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(LocalResourceDataProvider)
        builder.addConstructorArgReference("${config.name} data extractor")
        builder.addConstructorArgValue(config.id)
        beanDefinitionRegistry.registerBeanDefinition("${config.name} dataprovider", builder.beanDefinition)
    }

    private void createWebResourceDataProviderForConference(ConferencesConfiguration.Conference config, BeanDefinitionRegistry beanDefinitionRegistry) {
        BeanDefinitionBuilder builderDataProviderRemote = BeanDefinitionBuilder.genericBeanDefinition(WebResourceDataProviderRemote)
        builderDataProviderRemote.addConstructorArgValue(config)
        builderDataProviderRemote.addConstructorArgReference("${config.name} data extractor")
        beanDefinitionRegistry.registerBeanDefinition("${config.name} dataprovider remote", builderDataProviderRemote.beanDefinition)

        BeanDefinitionBuilder builderDataProvider = BeanDefinitionBuilder.genericBeanDefinition(WebResourceDataProvider)
        builderDataProvider.addConstructorArgReference("${config.name} dataprovider remote")
        builderDataProvider.addConstructorArgValue(config.id)
        beanDefinitionRegistry.registerBeanDefinition("${config.name} dataprovider", builderDataProvider.beanDefinition)

        BeanDefinitionBuilder builderHealthCheck = BeanDefinitionBuilder.genericBeanDefinition(WebResourceDataProviderHealthIndicator)
        builderHealthCheck.addConstructorArgReference("${config.name} dataprovider")
        builderHealthCheck.addConstructorArgReference("${config.name} dataprovider remote")
        beanDefinitionRegistry.registerBeanDefinition("${config.name} dataprovider health indicator", builderHealthCheck.beanDefinition)
    }

    @Override
    void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
    }
}
