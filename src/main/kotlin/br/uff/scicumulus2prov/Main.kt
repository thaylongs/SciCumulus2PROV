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
package br.uff.scicumulus2prov

import br.uff.scicumulus2prov.core.SciCumulus2PROV
import com.beust.jcommander.Parameter
import java.io.File

/**
 * @author Thaylon Guedes Santos
 * @email thaylongs@gmail.com
 */
class Args {
    @Parameter(names = arrayOf("--xml"), required = true, description = "The Xml configuration of SciCumulus")
    var xml: String? = null
    @Parameter(names = arrayOf("--prov"), required = true, description = "The PROV-N file name for write the provenance extracted from database")
    var prov: String? = null
    @Parameter(names = arrayOf("-h"), help = true, description = "Show this help text")
    var needHelp: Boolean = false
}

class BasicInformation(val dbName: String,
                       val dbPassword: String,
                       val dbUsername: String,
                       val dbHostname: String,
                       val dbPort: Int,
                       val workflowTag: String,
                       val execTag: String)

fun main(argv: Array<String>) {
//    val args = Args()
//    val jCommander = JCommander(args)
//    jCommander.setProgramName("SciCumulus2PROV")
//    try {
//        jCommander.parse(*argv)
//        if (args.needHelp) {
//            jCommander.usage()
//            return
//        }
//    } catch (e: Exception) {
//        jCommander.usage()
//        return
//    }
//    val dbConInfoWrapper = ExtractXmlInfo.getDataBaseConnectionFromXml(File(args.xml))
//    val dbConInfo = dbConInfoWrapper.orElseThrow { Exception("As configurações não foram encontradas no xml") }!!

    var basicInfo = BasicInformation(
//            dbName = "sciPhyTreeMiner",
            dbName = "32cores",
            dbPassword = "1234",
            dbUsername = "postgres",
            dbHostname = "localhost",
            dbPort = 5432,
            workflowTag = "sciphytreeminer",
            execTag = "wftreeminer_1-1")

    SciCumulus2PROV(basicInfo,File("output.provn")).start()
}