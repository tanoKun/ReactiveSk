package com.github.tanokun.reactivesk.skriptadapter.v2_6_3

import com.github.tanokun.reactivesk.skriptadapter.common.KoinModule
import com.github.tanokun.reactivesk.skriptadapter.common.LoadItemsAdapter
import com.github.tanokun.reactivesk.skriptadapter.common.analyze.ast.SkriptAstBuilder
import com.github.tanokun.reactivesk.skriptadapter.v2_6_3.analyze.ast.ConditionalParser
import com.github.tanokun.reactivesk.skriptadapter.v2_6_3.analyze.ast.LoopParser
import com.github.tanokun.reactivesk.skriptadapter.v2_6_3.analyze.ast.OtherSectionParser
import org.koin.dsl.module

object KoinModuleV263: KoinModule {
    override val module = module {
        single { SkriptAdapterV263 }
        single { LoadItemsAdapterV263 }
        single {
            SkriptAstBuilder(
                listOf(
                    ConditionalParser(),
                    LoopParser(),
                    OtherSectionParser()
                ),
                get<LoadItemsAdapter>()
            )
        }
    }
}