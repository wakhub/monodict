/**
 * Copyright (C) 2014 wak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.wakhub.monodict.dice;

import java.util.HashMap;

public interface Idice {
	IdicInfo open(String filename);

	int getDicNum();

	boolean isEnable(int num);

	void search(int num, String word);

//	boolean conjugationSearch(int num, String word);

	boolean isMatch(int num);

	IdicResult getResult(int num);

	IdicResult getMoreResult(int num);

	boolean hasMoreResult(int num);

	void close(IdicInfo info);

	IdicInfo getDicInfo(int num);

	IdicInfo getDicInfo(String filename);

	void 	setIrreg(HashMap<String, String> irreg);

	void	swap(IdicInfo info , int dir);
}
