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

package org.apache.inlong.sdk.transform.process.function.arithmetic;

import org.apache.inlong.sdk.transform.decode.SourceData;
import org.apache.inlong.sdk.transform.process.Context;
import org.apache.inlong.sdk.transform.process.function.FunctionConstant;
import org.apache.inlong.sdk.transform.process.function.TransformFunction;
import org.apache.inlong.sdk.transform.process.operator.OperatorTools;
import org.apache.inlong.sdk.transform.process.parser.ValueParser;

import net.sf.jsqlparser.expression.Function;

import java.math.BigDecimal;

/**
 * SignFunction  ->  sign(x)
 * description:
 * - Return NULL if 'x' is NULL
 * - Return -1 if 'x' is a negative number
 * - Return 0 if 'x' is equal to 0
 * - Return 1 if 'x' is a positive number
 */
@TransformFunction(type = FunctionConstant.ARITHMETIC_TYPE, names = {
        "sign"}, parameter = "(Numeric x)", descriptions = {
                "- Return \"\" if 'x' is NULL;",
                "- Return -1 if 'x' is a negative number;",
                "- Return 0 if 'x' is equal to 0;",
                "- Return 1 if 'x' is a positive number."
        }, examples = {"sign(-3.5) = -1"})
public class SignFunction implements ValueParser {

    private ValueParser numberParser;

    public SignFunction(Function expr) {
        numberParser = OperatorTools.buildParser(expr.getParameters().getExpressions().get(0));
    }

    @Override
    public Object parse(SourceData sourceData, int rowIndex, Context context) {
        Object numberObj = numberParser.parse(sourceData, rowIndex, context);
        if (numberObj == null) {
            return null;
        }
        BigDecimal numberValue = OperatorTools.parseBigDecimal(numberObj);
        double value = numberValue.doubleValue();
        if (value > 0) {
            return 1;
        } else if (value < 0) {
            return -1;
        }
        return 0;
    }
}
