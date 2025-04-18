/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.action.execution.api.model;

import org.wso2.carbon.identity.action.management.api.model.Action;

/**
 * This class models the Action Type.
 * Action Type is the type of the action that is executed by the Action Executor Service.
 */
public enum ActionType {
    PRE_ISSUE_ACCESS_TOKEN,
    PRE_UPDATE_PASSWORD,
    PRE_UPDATE_PROFILE,
    AUTHENTICATION;

    public String getDisplayName() {

        return Action.ActionTypes.valueOf(this.name()).getDisplayName();
    }
}
