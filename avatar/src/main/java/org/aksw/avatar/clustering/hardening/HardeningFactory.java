/*
 * #%L
 * AVATAR
 * %%
 * Copyright (C) 2015 Agile Knowledge Engineering and Semantic Web (AKSW)
 * %%
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
 * #L%
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.avatar.clustering.hardening;

/**
 *
 * @author ngonga
 */
public class HardeningFactory {
    public enum HardeningType { LARGEST, SMALLEST, AVERAGE};
    public static Hardening getHardening(HardeningType type)
    {
        if(type.equals(HardeningType.LARGEST)) return new LargestClusterHardening();
        if(type.equals(HardeningType.SMALLEST)) return new SmallestClusterHardening();
        if(type.equals(HardeningType.AVERAGE)) return new AverageWeightClusterHardening();
        else return new LargestClusterHardening();
    }
}
