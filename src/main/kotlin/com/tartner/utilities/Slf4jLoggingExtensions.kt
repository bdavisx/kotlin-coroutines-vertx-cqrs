/*
 * Copyright (c) 2019 Bill Davis.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   - http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.tartner.utilities

import org.slf4j.Logger

inline fun Logger.infoIf(messageProvider: () -> String) {
  if(isInfoEnabled) info(messageProvider())
}

inline fun Logger.infoIf(t: Throwable, messageProvider: () -> String) {
  if(isInfoEnabled) info(messageProvider(), t)
}

inline fun Logger.debugIf(messageProvider: () -> String) {
  if(isDebugEnabled) debug(messageProvider())
}

inline fun Logger.debugIf(t: Throwable, messageProvider: () -> String) {
  if(isDebugEnabled) debug(messageProvider(), t)
}

inline fun Logger.traceIf(messageProvider: () -> String) {
  if(isTraceEnabled) trace(messageProvider())
}

inline fun Logger.traceIf(t: Throwable, messageProvider: () -> String) {
  if(isTraceEnabled) trace(messageProvider(), t)
}
