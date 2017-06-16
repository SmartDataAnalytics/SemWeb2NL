/*
 * #%L
 * ASSESS
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
package org.aksw.assessment.util;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class PermutationsOfN {

  public static <T> List<List<T>> getSubsetsOfSizeN( List<T> set, int k ) {
    if ( k > set.size() ) {
      k = set.size();
    }
    List<List<T>> result = Lists.newArrayList();
    List<T> subset = Lists.newArrayListWithCapacity( k );
    for ( int i = 0; i < k; i++ ) {
      subset.add( null );
    }
    return processLargerSubsets( result, set, subset, 0, 0 );
  }

  private static <T> List<List<T>> processLargerSubsets( List<List<T>> result, List<T> set, List<T> subset, int subsetSize, int nextIndex ) {
    if ( subsetSize == subset.size() ) {
      result.add( ImmutableList.copyOf( subset ) );
    } else {
      for ( int j = nextIndex; j < set.size(); j++ ) {
        subset.set( subsetSize, set.get( j ) );
        processLargerSubsets( result, set, subset, subsetSize + 1, j + 1 );
      }
    }
    return result;
  }

  public static <T> Collection<List<T>> getPermutationsOfSizeN( List<T> list, int size ) {
    Collection<List<T>> all = Lists.newArrayList();
    if ( list.size() < size ) {
      size = list.size();
    }
    if ( list.size() == size ) {
      all.addAll( Collections2.permutations( list ) );
    } else {
      for ( List<T> p : getSubsetsOfSizeN( list, size ) ) {
        all.addAll( Collections2.permutations( p ) );
      }
    }
    return all;
  }
}