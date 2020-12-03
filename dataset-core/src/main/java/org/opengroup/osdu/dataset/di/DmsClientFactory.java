// Copyright © 2020 Amazon Web Services
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.dataset.di;

import org.opengroup.osdu.dataset.dms.DmsAPIConfig;
import org.opengroup.osdu.dataset.dms.DmsFactory;
import org.opengroup.osdu.dataset.dms.IDmsFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class DmsClientFactory extends AbstractFactoryBean<IDmsFactory> {
    
    @Value("${DMS_API_BASE}")
	private String DMS_API_BASE;

	@Override
	public Class<?> getObjectType() {
		return IDmsFactory.class;
	}

	@Override
	protected IDmsFactory createInstance() throws Exception {
		return new DmsFactory(DmsAPIConfig
				.builder()
				.rootUrl(DMS_API_BASE)
				.build());
	}
}
