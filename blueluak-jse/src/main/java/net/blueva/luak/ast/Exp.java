/******************************************************************************
 *  ____  _            _                _  __
 * | __ )| |_   _  ___| |   _   _  __ _| |/ /
 * |  _ \| | | | |/ _ \ |  | | | |/ _` | ' /
 * | |_) | | |_| |  __/ |__| |_| | (_| | . \
 * |____/|_|\__,_|\___|_____\__,_|\__,_|_|\_\
 *
 *  BlueLuaK
 *  https://github.com/BluevaDevelopment/BlueLuaK
 *
 *  Based on LuaJ (https://luaj.org)
 *  Original work Copyright (c) 2009 Luaj.org
 *  Modifications Copyright (c) 2026 Blueva Development
 *
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package net.blueva.luak.ast;

import net.blueva.luak.Lua;
import net.blueva.luak.LuaValue;

abstract
public class Exp extends SyntaxElement {
	abstract public void accept(Visitor visitor);

	public static Exp constant(LuaValue value) {
		return new Constant(value);
	}

	public static Exp numberconstant(String token) {		
		return new Constant( LuaValue.valueOf(token).tonumber() );
	}

	public static Exp varargs() {
		return new VarargsExp();
	}

	public static Exp tableconstructor(TableConstructor tc) {
		return tc;
	}

	public static Exp unaryexp(int op, Exp rhs) {
		if ( rhs instanceof BinopExp ) {
			BinopExp b = (BinopExp) rhs;
			if ( precedence(op) > precedence(b.op)  )
				return binaryexp( unaryexp(op, b.lhs), b.op, b.rhs );
		}
		return new UnopExp(op, rhs);
	}

	public static Exp binaryexp(Exp lhs, int op, Exp rhs) {
		if ( lhs instanceof UnopExp ) {
			UnopExp u = (UnopExp) lhs;
			if ( precedence(op) > precedence(u.op) )
				return unaryexp( u.op, binaryexp( u.rhs, op, rhs ) );
		}
		// TODO: cumulate string concatenations together
		// TODO: constant folding
		if ( lhs instanceof BinopExp ) {
			BinopExp b = (BinopExp) lhs;
			if ( (precedence(op) > precedence(b.op)) ||
				 ((precedence(op) == precedence(b.op)) && isrightassoc(op)) )
				return binaryexp( b.lhs, b.op, binaryexp( b.rhs, op, rhs ) );
		}
		if ( rhs instanceof BinopExp ) {
			BinopExp b = (BinopExp) rhs;
			if ( (precedence(op) > precedence(b.op)) ||
				 ((precedence(op) == precedence(b.op)) && ! isrightassoc(op)) )
				return binaryexp( binaryexp( lhs, op, b.lhs ), b.op, b.rhs );
		}
		return new BinopExp(lhs, op, rhs);
	}

	static boolean isrightassoc(int op) {
		switch ( op ) {
		case Lua.OP_CONCAT:
		case Lua.OP_POW: return true;
		default: return false;
		}
	}
	
	static int precedence(int op) {
		switch ( op ) {
		case Lua.OP_OR: return 0;
		case Lua.OP_AND: return 1;
		case Lua.OP_LT: case Lua.OP_GT: case Lua.OP_LE: case Lua.OP_GE: case Lua.OP_NEQ: case Lua.OP_EQ: return 2;
		case Lua.OP_CONCAT: return 3;
		case Lua.OP_ADD: case Lua.OP_SUB: return 4;
		case Lua.OP_MUL: case Lua.OP_DIV: case Lua.OP_MOD: return 5;
		case Lua.OP_NOT: case Lua.OP_UNM: case Lua.OP_LEN: return 6;
		case Lua.OP_POW: return 7;
		default: throw new IllegalStateException("precedence of bad op "+op);
		}
	}	
	
	public static Exp anonymousfunction(FuncBody funcbody) {
		return new AnonFuncDef(funcbody);
	}

	/** foo */
	public static NameExp nameprefix(String name) {
		return new NameExp(name);
	}

	/** ( foo.bar ) */
	public static ParensExp parensprefix(Exp exp) {
		return new ParensExp(exp);
	}

	/** foo[exp] */
	public static IndexExp indexop(PrimaryExp lhs, Exp exp) {
		return new IndexExp(lhs, exp);
	}

	/** foo.bar */
	public static FieldExp fieldop(PrimaryExp lhs, String name) {
		return new FieldExp(lhs, name);
	}

	/** foo(2,3) */
	public static FuncCall functionop(PrimaryExp lhs, FuncArgs args) {
		return new FuncCall(lhs, args);
	}

	/** foo:bar(4,5) */
	public static MethodCall methodop(PrimaryExp lhs, String name, FuncArgs args) {
		return new MethodCall(lhs, name, args);
	}

	public boolean isvarexp() {
		return false;
	}

	public boolean isfunccall() {
		return false;
	}

	public boolean isvarargexp() {
		return false;
	}
	
	abstract public static class PrimaryExp extends Exp {
		public boolean isvarexp() {
			return false;
		}
		public boolean isfunccall() {
			return false;
		}
	}

	abstract public static class VarExp extends PrimaryExp {
		public boolean isvarexp() {
			return true;
		}
		public void markHasAssignment() {
		}
	}
	
	public static class NameExp extends VarExp {
		public final Name name;
		public NameExp(String name) {
			this.name = new Name(name);
		}
		public void markHasAssignment() {
			name.variable.hasassignments = true;
		}
		public void accept(Visitor visitor) {
			visitor.visit(this);
		}
	}
	
	public static class ParensExp extends PrimaryExp {
		public final Exp exp;
		public ParensExp(Exp exp) {
			this.exp = exp;
		}
		
		public void accept(Visitor visitor) {
			visitor.visit(this);
		}
	}
	
	public static class FieldExp extends VarExp {
		public final PrimaryExp lhs;
		public final Name name;
		public FieldExp(PrimaryExp lhs, String name) {
			this.lhs = lhs;
			this.name = new Name(name);
		}
		
		public void accept(Visitor visitor) {
			visitor.visit(this);
		}
	}
	
	public static class IndexExp extends VarExp {
		public final PrimaryExp lhs;
		public final Exp exp;
		public IndexExp(PrimaryExp lhs, Exp exp) {
			this.lhs = lhs;
			this.exp = exp;
		}
		
		public void accept(Visitor visitor) {
			visitor.visit(this);
		}
	}
	
	public static class FuncCall extends PrimaryExp {
		public final PrimaryExp lhs;
		public final FuncArgs args;
		
		public FuncCall(PrimaryExp lhs, FuncArgs args) {
			this.lhs = lhs;
			this.args = args;
		}

		public boolean isfunccall() {
			return true;
		}
		
		public void accept(Visitor visitor) {
			visitor.visit(this);
		}
		
		public boolean isvarargexp() {
			return true;
		}
	}
	
	public static class MethodCall extends FuncCall {
		public final String name;
		
		public MethodCall(PrimaryExp lhs, String name, FuncArgs args) {
			super(lhs, args);
			this.name = new String(name);
		}

		public boolean isfunccall() {
			return true;
		}
		
		public void accept(Visitor visitor) {
			visitor.visit(this);
		}
	}

	public static class Constant extends Exp {
		public final LuaValue value;
		public Constant(LuaValue value) {
			this.value = value;
		}

		public void accept(Visitor visitor) {
			visitor.visit(this);
		}		
	}

	public static class VarargsExp extends Exp {
		
		public void accept(Visitor visitor) {
			visitor.visit(this);
		}
		
		public boolean isvarargexp() {
			return true;
		}
	}
	
	public static class UnopExp extends Exp {
		public final int op;
		public final Exp rhs;
		public UnopExp(int op, Exp rhs) {
			this.op = op;
			this.rhs = rhs;
		}

		public void accept(Visitor visitor) {
			visitor.visit(this);
		}		
	}
	
	public static class BinopExp extends Exp {
		public final Exp lhs,rhs;
		public final int op;
		public BinopExp(Exp lhs, int op, Exp rhs) {
			this.lhs = lhs;
			this.op = op;
			this.rhs = rhs;
		}

		public void accept(Visitor visitor) {
			visitor.visit(this);
		}		
	}
	
	public static class AnonFuncDef extends Exp {
		public final FuncBody body;
		public AnonFuncDef(FuncBody funcbody) {
			this.body = funcbody;
		}

		public void accept(Visitor visitor) {
			visitor.visit(this);
		}		
	}

}
