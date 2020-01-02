package com.small.config.core;

import com.small.config.SmallConfClient;
import com.small.config.annotation.SmallConf;
import com.small.config.core.factory.SmallConfBaseFactory;
import com.small.config.core.listener.impl.BeanRefreshSmallConfListener;
import com.small.config.util.FieldReflectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.*;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;

/**
 * @author null
 * @version 1.0
 * @title
 * @description
 * @createDate 1/2/20 4:09 PM
 */
public class SmallConfFactory extends InstantiationAwareBeanPostProcessorAdapter
        implements InitializingBean, DisposableBean, BeanNameAware, BeanFactoryAware {

    private static Logger logger = LoggerFactory.getLogger(SmallConfFactory.class);


    // ---------------------- env config ----------------------

    // like "small-conf.properties" or "file:/data/webapps/small-conf.properties",
    // include the following env config
    private String envprop;
    private String adminAddress;
    private String env;
    private String accessToken;
    private String mirrorfile;
    private String beanName;
    private static BeanFactory beanFactory;

    public void setAdminAddress(String adminAddress) {
        this.adminAddress = adminAddress;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setMirrorfile(String mirrorfile) {
        this.mirrorfile = mirrorfile;
    }


    // ---------------------- post process / xml、annotation ----------------------

    @Override
    public boolean postProcessAfterInstantiation(final Object bean, final String beanName) throws BeansException {

        // 1、Annotation('@XxlConf')：resolves conf + watch
        if (!beanName.equals(this.beanName)) {
            ReflectionUtils.doWithFields(bean.getClass(), field -> {
                if (field.isAnnotationPresent(SmallConf.class)) {
                    String propertyName = field.getName();
                    SmallConf smallConf = field.getAnnotation(SmallConf.class);

                    String confKey = smallConf.value();
                    String confValue = SmallConfClient.get(confKey, smallConf.defaultValue());


                    // resolves placeholders
                    BeanRefreshSmallConfListener.BeanField beanField = new BeanRefreshSmallConfListener.BeanField(beanName, propertyName);
                    refreshBeanField(beanField, confValue, bean);

                    // watch
                    if (smallConf.callback()) {
                        BeanRefreshSmallConfListener.addBeanField(confKey, beanField);
                    }

                }
            });
        }

        return super.postProcessAfterInstantiation(bean, beanName);
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {

        // 2、XML('$SmallConf{...}')：resolves placeholders + watch
        if (!beanName.equals(this.beanName)) {
            BeanWrapper beanWrapper = new BeanWrapperImpl(bean);
            PropertyDescriptor[] pds = beanWrapper.getPropertyDescriptors();

            PropertyValue[] pvArray = pvs.getPropertyValues();
            for (PropertyValue pv : pvArray) {
                if (pv.getValue() instanceof TypedStringValue) {
                    String propertyName = pv.getName();
                    String typeStringVal = ((TypedStringValue) pv.getValue()).getValue();
                    if (xmlKeyValid(typeStringVal)) {

                        // object + property
                        String confKey = xmlKeyParse(typeStringVal);
                        String confValue = SmallConfClient.get(confKey, "");

                        // resolves placeholders
                        BeanRefreshSmallConfListener.BeanField beanField = new BeanRefreshSmallConfListener.BeanField(beanName, propertyName);
                        //refreshBeanField(beanField, confValue, bean);

                        Class propClass = String.class;
                        for (PropertyDescriptor item : pds) {
                            if (beanField.getProperty().equals(item.getName())) {
                                propClass = item.getPropertyType();
                            }
                        }
                        Object valueObj = FieldReflectionUtil.parseValue(propClass, confValue);
                        pv.setConvertedValue(valueObj);

                        // watch
                        BeanRefreshSmallConfListener.addBeanField(confKey, beanField);
                    }
                }
            }
        }

        return super.postProcessProperties(pvs, bean, beanName);
    }

    private static final String placeholderPrefix = "$SmallConf{";
    private static final String placeholderSuffix = "}";

    /**
     * valid xml
     *
     * @param originKey
     * @return
     */
    private static boolean xmlKeyValid(String originKey) {
        boolean start = originKey.startsWith(placeholderPrefix);
        boolean end = originKey.endsWith(placeholderSuffix);
        if (start && end) {
            return true;
        }
        return false;
    }

    /**
     * parse xml
     *
     * @param originKey
     * @return
     */
    private static String xmlKeyParse(String originKey) {
        if (xmlKeyValid(originKey)) {
            // replace by small-conf
            String key = originKey.substring(placeholderPrefix.length(), originKey.length() - placeholderSuffix.length());
            return key;
        }
        return null;
    }


    /**
     * refresh bean with small conf (fieldNames)
     */
    public static void refreshBeanField(final BeanRefreshSmallConfListener.BeanField beanField, final String value, Object bean) {
        if (bean == null) {
            bean = SmallConfFactory.beanFactory.getBean(beanField.getBeanName());        // 已优化：启动时禁止实用，getBean 会导致Bean提前初始化，风险较大；
        }
        if (bean == null) {
            return;
        }

        BeanWrapper beanWrapper = new BeanWrapperImpl(bean);

        // property descriptor
        PropertyDescriptor propertyDescriptor = null;
        PropertyDescriptor[] propertyDescriptors = beanWrapper.getPropertyDescriptors();
        if (propertyDescriptors != null && propertyDescriptors.length > 0) {
            for (PropertyDescriptor item : propertyDescriptors) {
                if (beanField.getProperty().equals(item.getName())) {
                    propertyDescriptor = item;
                }
            }
        }

        // refresh field: set or field
        if (propertyDescriptor != null && propertyDescriptor.getWriteMethod() != null) {
            beanWrapper.setPropertyValue(beanField.getProperty(), value);    // support mult data types
            logger.info(">>>>>>>>>>> small-conf, refreshBeanField[set] success, {}#{}:{}",
                    beanField.getBeanName(), beanField.getProperty(), value);
        } else {

            final Object finalBean = bean;
            ReflectionUtils.doWithFields(bean.getClass(), fieldItem -> {
                if (beanField.getProperty().equals(fieldItem.getName())) {
                    try {
                        Object valueObj = FieldReflectionUtil.parseValue(fieldItem.getType(), value);

                        fieldItem.setAccessible(true);
                        fieldItem.set(finalBean, valueObj);        // support mult data types

                        logger.info(">>>>>>>>>>> small-conf, refreshBeanField[field] success, {}#{}:{}",
                                beanField.getBeanName(), beanField.getProperty(), value);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

    }


    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        SmallConfFactory.beanFactory = beanFactory;
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    @Override
    public void destroy() throws Exception {
        SmallConfBaseFactory.destroy();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        SmallConfBaseFactory.init(adminAddress, env, accessToken, mirrorfile);
    }
}
