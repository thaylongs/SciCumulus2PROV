/*
 Copyright (c) 2017 Thaylon Guedes Santos and Eduardo Smil Prutchi

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package br.uff.scicumulus2prov.model

import java.sql.Timestamp
import java.time.LocalDateTime

/**
 * @author Thaylon Guedes Santos
 * @email thaylongs@gmail.com
 */
data class EWorkflow(val ewkfid: Int,
                     val tagexec: String,
                     val expdir: String,
                     val wfdir: String,
                     val tag: String,
                     val maximumfailures: Int,
                     val userinteraction: String,
                     val reliability: Double,
                     val redundancy: String)

data class EActivity(val actid: Int,
                     val wkfid: Int,
                     val tag: String,
                     val status: String,
                     val starttime: Timestamp,
                     val endtime: Timestamp,
                     val cactid: Int,
                     val templatedir: String)