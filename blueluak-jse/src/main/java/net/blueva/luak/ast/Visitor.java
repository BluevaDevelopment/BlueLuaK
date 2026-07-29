/******************************************************************************
 *  ____  _            _                    _
 * | __ )| |_   _  ___| |   _   _  __ _    | |
 * |  _ \| | | | |/ _ \ |  | | | |/ _` |_  | |
 * | |_) | | |_| |  __/ |__| |_| | (_| | |_| |
 * |____/|_|\__,_|\___|_____\__,_|\__,_|\___/
 *
 *  BlueLuaK
 *  https://github.com/BluevaDevelopment/BlueLuaK/t
 *
 *  Based on LuaJ (https://luaj.org)
 *  Original work Copyright (c) 2009 Luaj.org
 *  Modifications Copyright (c) 2026 Blueva Development
 *
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package net.blueva.luak.ast;

import java.util.List;

import net.blueva.luak.ast.Exp.VarExp;

abstract public class Visitor {
	public void visit(Chunk chunk) { 
		chunk.block.accept(this); 
	};
	public void visit(Block block) {
		visit(block.scope);
		if ( block.stats != null )
			for ( int i=0, n=block.stats.size(); i<n; i++ )
				((Stat)block.stats.get(i)).accept(this);
	};
	public void visit(Stat.Assign stat) {
		visitVars(stat.vars);
		visitExps(stat.exps);
	}
	public void visit(Stat.Break breakstat) {
	}
	public void visit(Stat.FuncCallStat stat) {
		stat.funccall.accept(this);
	}
	public void visit(Stat.FuncDef stat) {
		stat.body.accept(this);
	}
	public void visit(Stat.GenericFor stat) {
		visit(stat.scope);
		visitNames(stat.names);
		visitExps(stat.exps);
		stat.block.accept(this);
	}
	public void visit(Stat.IfThenElse stat) {
		stat.ifexp.accept(this);
		stat.ifblock.accept(this);
		if ( stat.elseifblocks != null ) 
			for ( int i=0, n=stat.elseifblocks.size(); i<n; i++ ) {
				((Exp)stat.elseifexps.get(i)).accept(this);
				((Block)stat.elseifblocks.get(i)).accept(this);
			}
		if ( stat.elseblock != null )
			visit( stat.elseblock );
	}
	public void visit(Stat.LocalAssign stat) {
		visitNames(stat.names);
		visitExps(stat.values);
	}
	public void visit(Stat.LocalFuncDef stat) {
		visit(stat.name);
		stat.body.accept(this);
	}
	public void visit(Stat.NumericFor stat) {
		visit(stat.scope);
		visit(stat.name);
		stat.initial.accept(this);
		stat.limit.accept(this);
		if ( stat.step != null )
			stat.step.accept(this);
		stat.block.accept(this);
	}
	public void visit(Stat.RepeatUntil stat) {
		stat.block.accept(this);
		stat.exp.accept(this);
	}
	public void visit(Stat.Return stat) {
		visitExps(stat.values);
	}
	public void visit(Stat.WhileDo stat) {
		stat.exp.accept(this);
		stat.block.accept(this);
	}
	public void visit(FuncBody body) {
		visit(body.scope);
		body.parlist.accept(this);
		body.block.accept(this);
	}
	public void visit(FuncArgs args) {
		visitExps(args.exps);
	}
	public void visit(TableField field) {
		if ( field.name != null )
			visit( field.name );
		if ( field.index != null )
			field.index.accept(this);
		field.rhs.accept(this);
	}
	public void visit(Exp.AnonFuncDef exp) {
		exp.body.accept(this);
	}
	public void visit(Exp.BinopExp exp) {
		exp.lhs.accept(this);
		exp.rhs.accept(this);
	}
	public void visit(Exp.Constant exp) {
	}
	public void visit(Exp.FieldExp exp) {
		exp.lhs.accept(this);
		visit(exp.name);
	}
	public void visit(Exp.FuncCall exp) {
		exp.lhs.accept(this);
		exp.args.accept(this);
	}
	public void visit(Exp.IndexExp exp) {
		exp.lhs.accept(this);
		exp.exp.accept(this);
	}
	public void visit(Exp.MethodCall exp) {
		exp.lhs.accept(this);
		visit(exp.name);
		exp.args.accept(this);
	}
	public void visit(Exp.NameExp exp) {
		visit(exp.name);
	}
	public void visit(Exp.ParensExp exp) {
		exp.exp.accept(this);
	}
	public void visit(Exp.UnopExp exp) {
		exp.rhs.accept(this);
	}
	public void visit(Exp.VarargsExp exp) {
	}
	public void visit(ParList pars) {
		visitNames(pars.names);
	}
	public void visit(TableConstructor table) {
		if( table.fields != null)
			for ( int i=0, n=table.fields.size(); i<n; i++ )
				((TableField)table.fields.get(i)).accept(this);
	}
	public void visitVars(List<VarExp> vars) {
		if ( vars != null )
			for ( int i=0, n=vars.size(); i<n; i++ )
				((Exp.VarExp)vars.get(i)).accept(this);
	}
	public void visitExps(List<Exp> exps) {
		if ( exps != null )
			for ( int i=0, n=exps.size(); i<n; i++ )
				((Exp)exps.get(i)).accept(this);
	}
	public void visitNames(List<Name> names) {
		if ( names != null )
			for ( int i=0, n=names.size(); i<n; i++ )
				visit((Name) names.get(i));
	}
	public void visit(Name name) {
	}
	public void visit(String name) {
	}
	public void visit(NameScope scope) {
	}
	public void visit(Stat.Goto gotostat) {
	}
	public void visit(Stat.Label label) {
	}
}
