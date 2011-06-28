package org.antlr.v4.codegen;

import org.antlr.v4.codegen.model.*;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.tool.*;

import java.util.*;

/** Create output objects for elements *within* rule functions except
 *  buildOutputModel() which builds outer/root model object and any
 *  objects such as RuleFunction that surround elements in rule
 *  functions.
 */
public abstract class DefaultOutputModelFactory implements OutputModelFactory {
	// Interface to outside world
	public Grammar g;
	public CodeGenerator gen;

	// Context ptrs
	public OutputModelObject root; // normally ParserFile, LexerFile, ...
	public Stack<RuleFunction> currentRule = new Stack<RuleFunction>();
	public Alternative currentAlt;

	protected DefaultOutputModelFactory(CodeGenerator gen) {
		this.gen = gen;
		this.g = gen.g;
	}

	public Grammar getGrammar() { return g; }

	public CodeGenerator getGenerator() { return gen; }

	public ParserFile parserFile(String fileName) {
		return new ParserFile(this, fileName);
	}

	public Parser parser(ParserFile file) {
		return new Parser(this, file);
	}

	public RuleFunction rule(Rule r) {
		return new RuleFunction(this, r);
	}

	public List<SrcOp> rulePostamble(RuleFunction function, Rule r) {
		return null;
	}

	// ALTERNATIVES / ELEMENTS

	public List<SrcOp> alternative(List<SrcOp> elems) { return null; }

	public List<SrcOp> ruleRef(GrammarAST ID, GrammarAST label, GrammarAST args) { return null; }

	public List<SrcOp> tokenRef(GrammarAST ID, GrammarAST label, GrammarAST args) { return null; }

	public List<SrcOp> stringRef(GrammarAST ID, GrammarAST label) { return null; }

	public List<SrcOp> epsilon() { return null; }

	// ACTIONS

	public List<SrcOp> action(GrammarAST ast) { return null; }

	public List<SrcOp> forcedAction(GrammarAST ast) { return null; }

	public List<SrcOp> sempred(GrammarAST ast) { return null; }

	// AST OPS

	public List<SrcOp> rootToken(List<SrcOp> ops) { return ops; }

	public List<SrcOp> rootRule(List<SrcOp> ops) { return ops; }

	// BLOCKS

	public List<SrcOp> getChoiceBlock(BlockAST blkAST, List<SrcOp> alts) { return null; }

	public List<SrcOp> getEBNFBlock(GrammarAST ebnfRoot, List<SrcOp> alts) { return null; }

	public List<SrcOp> getLL1ChoiceBlock(BlockAST blkAST, List<SrcOp> alts) { return null; }

	public List<SrcOp> getLLStarChoiceBlock(BlockAST blkAST, List<SrcOp> alts) { return null; }

	public List<SrcOp> getLL1EBNFBlock(GrammarAST ebnfRoot, List<SrcOp> alts) { return null; }

	public List<SrcOp> getLLStarEBNFBlock(GrammarAST ebnfRoot, List<SrcOp> alts) { return null; }

	public List<SrcOp> getLL1Test(IntervalSet look, GrammarAST blkAST) { return null; }

	// SET CONTEXT

	public OutputModelObject getRoot() { return root; }

	public void setRoot(OutputModelObject root) { this.root = root;	}

	public RuleFunction getCurrentRule() {
		if ( currentRule.size()>0 )	return currentRule.peek();
		return null;
	}

	public void pushCurrentRule(RuleFunction r) { currentRule.push(r); }

	public RuleFunction popCurrentRule() {
		if ( currentRule.size()>0 ) return currentRule.pop();
		return null;
	}

	public Alternative getCurrentAlt() { return currentAlt; }

	public void setCurrentAlt(Alternative currentAlt) { this.currentAlt = currentAlt; }

	// MISC

	public static List<SrcOp> list(Object... values) {
		List<SrcOp> x = new ArrayList<SrcOp>(values.length);
		for (Object v : values) {
			if ( v!=null ) {
				if ( v instanceof List<?> ) x.addAll((List) v);
				else x.add((SrcOp)v);
			}
		}
		return x;
	}
}

