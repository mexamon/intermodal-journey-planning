package com.thy.cloud.data.cache.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.Assert;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PackageConfig {

	/** Basic package */
	@Setter
	private static String[] basePackages = null;
	
	public static String[] getBasePackages() {
		Assert.isTrue(ArrayUtils.isNotEmpty(PackageConfig.basePackages), "basePackages is not configured！！！");
		
		return basePackages;
	}

}