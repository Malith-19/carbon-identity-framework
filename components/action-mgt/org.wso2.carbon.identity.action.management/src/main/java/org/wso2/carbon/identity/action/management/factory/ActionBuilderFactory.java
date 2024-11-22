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

package org.wso2.carbon.identity.action.management.factory;

import org.wso2.carbon.identity.action.management.ActionBuilder;
import org.wso2.carbon.identity.action.management.model.Action;

import java.util.HashMap;
import java.util.Map;

/**
 * This class defines the Action Object Builder Factory.
 * Action Object Builder Factory is the component that is responsible for providing the {@link ActionBuilder}
 * based on the action type.
 */
public class ActionBuilderFactory {

    private static final Map<Action.ActionTypes, ActionBuilder> actionObjectBuilders = new HashMap<>();

    public static ActionBuilder getActionBuilder(Action.ActionTypes actionType) {

        switch (actionType) {
            case PRE_UPDATE_PASSWORD:
                return actionObjectBuilders.get(Action.ActionTypes.PRE_UPDATE_PASSWORD);
            case PRE_ISSUE_ACCESS_TOKEN:
            default:
                return null;
        }
    }

    public static void registerActionBuilder(ActionBuilder actionBuilder) {

        actionObjectBuilders.put(actionBuilder.getSupportedActionType(), actionBuilder);
    }

    public static void unregisterActionBuilder(ActionBuilder actionBuilder) {

        actionObjectBuilders.remove(actionBuilder.getSupportedActionType());
    }
}
