/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.transform.process.function.collection;

import org.apache.inlong.sdk.transform.decode.SourceData;
import org.apache.inlong.sdk.transform.process.Context;
import org.apache.inlong.sdk.transform.process.function.FunctionConstant;
import org.apache.inlong.sdk.transform.process.function.TransformFunction;
import org.apache.inlong.sdk.transform.process.operator.OperatorTools;
import org.apache.inlong.sdk.transform.process.parser.ValueParser;

import net.sf.jsqlparser.expression.Function;

import java.util.Arrays;
import java.util.Map;
/**
 * MapValueFunction  ->  MAP_VALUES(map)
 * description:
 * - Return NULL if map is null
 * - Return the values of the map as array. No order guaranteed.
 */
@TransformFunction(type = FunctionConstant.COLLECTION_TYPE, names = {
        "map_values"}, parameter = "(Map map)", descriptions = {
                "- Return \"\" if 'map' is null;",
                "- Return the values of the 'map' as array. No order guaranteed."
        }, examples = {
                "map_values(Map('he',1,'xxd','cloud')) = [1, cloud]",
                "map_values(Map('xxd','cloud',map(1,2),map(3,'apple'))) = [cloud, {3=apple}]"
        })
public class MapValueFunction implements ValueParser {

    private final ValueParser mapParser;

    public MapValueFunction(Function expr) {
        this.mapParser = OperatorTools.buildParser(expr.getParameters().getExpressions().get(0));
    }

    @Override
    public Object parse(SourceData sourceData, int rowIndex, Context context) {
        Object mapObj = mapParser.parse(sourceData, rowIndex, context);
        if (mapObj == null) {
            return null;
        }
        if (mapObj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) mapObj;
            if (map.isEmpty()) {
                return null;
            }
            return Arrays.toString(map.values().toArray());
        }
        return null;
    }

}