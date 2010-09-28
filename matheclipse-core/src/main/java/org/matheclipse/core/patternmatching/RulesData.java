package org.matheclipse.core.patternmatching;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matheclipse.basic.Config;
import org.matheclipse.core.eval.EvalEngine;
import org.matheclipse.core.eval.exception.RuleCreationError;
import org.matheclipse.core.expression.F;
import org.matheclipse.core.interfaces.IAST;
import org.matheclipse.core.interfaces.IEvaluationEngine;
import org.matheclipse.core.interfaces.IExpr;
import org.matheclipse.core.interfaces.IPattern;
import org.matheclipse.core.interfaces.IPatternMatcher;
import org.matheclipse.core.interfaces.ISymbol;
import org.matheclipse.generic.interfaces.Pair;

import com.google.common.collect.ArrayListMultimap;

/**
 * The pattern matching rules associated with a symbol.
 */
public class RulesData {
	private Map<IExpr, Pair<ISymbol, IExpr>> fEqualRules;
	private ArrayListMultimap<Integer, IPatternMatcher<IExpr>> fSimplePatternRules;
	private List<IPatternMatcher<IExpr>> fPatternRules;

	public RulesData() {
		this.fEqualRules = null;
		this.fSimplePatternRules = null;
		this.fPatternRules = null;
	}

	public void clear() {
		fEqualRules = null;
		fSimplePatternRules = null;
		fPatternRules = null;
	}

	public IExpr evalDownRule(final IExpr expression) {
		return evalDownRule(EvalEngine.get(), expression);
	}

	public IExpr evalDownRule(final IEvaluationEngine ee, final IExpr expression) {
		Pair<ISymbol, IExpr> res;
		if (fEqualRules != null) {
			res = fEqualRules.get(expression);
			if (res != null) {
				return res.getSecond();
			}
		}

		IExpr result;
		IPatternMatcher<IExpr> pmEvaluator;
		if ((fSimplePatternRules != null) && (expression instanceof IAST)) {
			final Integer hash = Integer.valueOf(((IAST) expression).patternHashCode());
			final List<IPatternMatcher<IExpr>> list = fSimplePatternRules.get(hash);
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					if (Config.SERVER_MODE) {
						pmEvaluator = (IPatternMatcher<IExpr>) list.get(i).clone();
					} else {
						pmEvaluator = list.get(i);
					}
					result = pmEvaluator.eval(expression);
					if (result != null) {
						return result;
					}
				}
			}
		}

		if (fPatternRules != null) {
			for (int i = 0; i < fPatternRules.size(); i++) {
				if (Config.SERVER_MODE) {
					pmEvaluator = (IPatternMatcher<IExpr>) fPatternRules.get(i).clone();
				} else {
					pmEvaluator = fPatternRules.get(i);
				}
				result = pmEvaluator.eval(expression);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	public PatternMatcher putDownRule(ISymbol setSymbol, final boolean equalRule, final IExpr leftHandSide,
			final IExpr rightHandSide, final IExpr condition, final int priority) {
		if (Config.DEBUG) {
			if (rightHandSide.isAST("Condition")) {
				throw new RuntimeException("Condition  not allowed in right-hand-side");
			}
		}
		if (equalRule) {
			fEqualRules = getEqualRules();
			fEqualRules.put(leftHandSide, new Pair<ISymbol, IExpr>(setSymbol, rightHandSide));
			if (condition != null) {
				throw new RuleCreationError(leftHandSide, rightHandSide, condition);
			}
			return null;
		}

		final PatternMatcherAndEvaluator pmEvaluator = new PatternMatcherAndEvaluator(setSymbol, leftHandSide, rightHandSide);

		if (pmEvaluator.isRuleWithoutPatterns()) {
			fEqualRules = getEqualRules();
			fEqualRules.put(leftHandSide, new Pair<ISymbol, IExpr>(setSymbol, rightHandSide));
			if (condition != null) {
				throw new RuleCreationError(leftHandSide, rightHandSide, condition);
			}
			return null;
		}

		// if (condition!=null && condition.isAST(F.And,3)) {
		// System.out.println("condition");
		// }
		pmEvaluator.setCondition(condition);
		if (!isComplicatedPatternRule(leftHandSide)) {

			fSimplePatternRules = getSimplePatternRules();
			return addSimplePatternRule(leftHandSide, pmEvaluator);

		} else {

			fPatternRules = getPatternRules();
			for (int i = 0; i < fPatternRules.size(); i++) {
				if (pmEvaluator.equals(fPatternRules.get(i))) {
					fPatternRules.set(i, pmEvaluator);

					return pmEvaluator;
				}
			}
			fPatternRules.add(pmEvaluator);
			return pmEvaluator;
		}

	}

	private PatternMatcher addSimplePatternRule(final IExpr leftHandSide, final PatternMatcher pmEvaluator) {
		final Integer hash = Integer.valueOf(((IAST) leftHandSide).patternHashCode());
		if (fSimplePatternRules.containsEntry(hash, pmEvaluator)) {
			fSimplePatternRules.remove(hash, pmEvaluator);
		}
		fSimplePatternRules.put(hash, pmEvaluator);
		return pmEvaluator;
	}

	/** {@inheritDoc} */
	public PatternMatcher putDownRule(final PatternMatcherAndInvoker pmEvaluator) {
		final IExpr leftHandSide = pmEvaluator.getLHS();
		if (!isComplicatedPatternRule(leftHandSide)) {

			fSimplePatternRules = getSimplePatternRules();
			return addSimplePatternRule(leftHandSide, pmEvaluator);

		} else {

			fPatternRules = getPatternRules();
			for (int i = 0; i < fPatternRules.size(); i++) {
				if (pmEvaluator.equals(fPatternRules.get(i))) {
					fPatternRules.set(i, pmEvaluator);
					return pmEvaluator;
				}
			}
			fPatternRules.add(pmEvaluator);
			return pmEvaluator;
		}
	}

	private boolean isComplicatedPatternRule(final IExpr patternExpr) {
		if (patternExpr instanceof IAST) {
			final IAST ast = ((IAST) patternExpr);
			if (ast.size() > 1) {
				if ((ast.get(1) instanceof IPattern)) {
					return true;
				}
				final int attr = ast.topHead().getAttributes();
				if ((ISymbol.ORDERLESS & attr) == ISymbol.ORDERLESS) {
					return true;
				}
				for (int i = 2; i < ast.size(); i++) {
					if (ast.get(i) instanceof IPattern && ((IPattern) ast.get(i)).isDefault()) {
						return true;
					}
				}
			}
		} else if (patternExpr instanceof IPattern) {
			return true;
		}
		return false;
	}

	/**
	 * @return Returns the equalRules.
	 */
	private Map<IExpr, Pair<ISymbol, IExpr>> getEqualRules() {
		if (fEqualRules == null) {
			fEqualRules = new HashMap<IExpr, Pair<ISymbol, IExpr>>();
		}
		return fEqualRules;
	}

	private List<IPatternMatcher<IExpr>> getPatternRules() {
		if (fPatternRules == null) {
			fPatternRules = new ArrayList<IPatternMatcher<IExpr>>();
		}
		return fPatternRules;
	}

	private ArrayListMultimap<Integer, IPatternMatcher<IExpr>> getSimplePatternRules() {
		if (fSimplePatternRules == null) {
			fSimplePatternRules = ArrayListMultimap.create();
		}
		return fSimplePatternRules;
	}

	public List<IAST> definition() {
		ArrayList<IAST> definitionList = new ArrayList<IAST>();
		Iterator<IExpr> iter;
		IExpr key;
		Pair<ISymbol, IExpr> pair;
		IExpr condition;
		ISymbol setSymbol;
		IAST ast;
		PatternMatcherAndEvaluator pmEvaluator;
		if (fEqualRules != null && fEqualRules.size() > 0) {
			iter = fEqualRules.keySet().iterator();
			while (iter.hasNext()) {
				key = iter.next();
				pair = fEqualRules.get(key);
				setSymbol = pair.getFirst();
				ast = F.ast(setSymbol);
				ast.add(key);
				ast.add(pair.getSecond());
				definitionList.add(ast);
			}
		}
		if (fSimplePatternRules != null && fSimplePatternRules.size() > 0) {
			Iterator<IPatternMatcher<IExpr>> listIter = fSimplePatternRules.values().iterator();
			IPatternMatcher<IExpr> elem;
			while (listIter.hasNext()) {
				elem = listIter.next();
				if (elem instanceof PatternMatcherAndEvaluator) {
					pmEvaluator = (PatternMatcherAndEvaluator) elem;
					setSymbol = pmEvaluator.getSetSymbol();

					ast = F.ast(setSymbol);
					ast.add(pmEvaluator.getLHS());
					condition = pmEvaluator.getCondition();
					if (condition != null) {
						ast.add(F.Condition(pmEvaluator.getRHS(), condition));
					} else {
						ast.add(pmEvaluator.getRHS());
					}
					definitionList.add(ast);
				}
				// if (elem instanceof PatternMatcherAndInvoker) {
				// don't show internal methods associated with a pattern
				// }
			}
		}
		if (fPatternRules != null && fPatternRules.size() > 0) {
			for (int i = 0; i < fPatternRules.size(); i++) {
				if (fPatternRules.get(i) instanceof PatternMatcherAndEvaluator) {
					pmEvaluator = (PatternMatcherAndEvaluator) fPatternRules.get(i);
					setSymbol = pmEvaluator.getSetSymbol();
					ast = F.ast(setSymbol);
					ast.add(pmEvaluator.getLHS());
					condition = pmEvaluator.getCondition();
					if (condition != null) {
						ast.add(F.Condition(pmEvaluator.getRHS(), condition));
					} else {
						ast.add(pmEvaluator.getRHS());
					}
					definitionList.add(ast);
				}
			}

		}

		return definitionList;
	}

	public void readSymbol(java.io.ObjectInputStream stream) throws IOException {

		String astString;
		IExpr key;
		IExpr value;
		EvalEngine engine = EvalEngine.get();
		ISymbol setSymbol;
		int len = stream.read();
		if (len > 0) {
			fEqualRules = new HashMap<IExpr, Pair<ISymbol, IExpr>>();
			for (int i = 0; i < len; i++) {
				astString = stream.readUTF();
				setSymbol = F.symbol(astString);

				astString = stream.readUTF();
				key = engine.parse(astString);
				astString = stream.readUTF();
				value = engine.parse(astString);
				fEqualRules.put(key, new Pair<ISymbol, IExpr>(setSymbol, value));
			}
		}

		len = stream.read();
		IExpr lhs;
		IExpr rhs;
		IExpr condition;
		int listLength;
		int condLength;
		PatternMatcherAndEvaluator pmEvaluator;
		if (len > 0) {
			fSimplePatternRules = ArrayListMultimap.create();
			for (int i = 0; i < len; i++) {
				astString = stream.readUTF();
				setSymbol = F.symbol(astString);

				astString = stream.readUTF();
				lhs = engine.parse(astString);
				astString = stream.readUTF();
				rhs = engine.parse(astString);
				pmEvaluator = new PatternMatcherAndEvaluator(setSymbol, lhs, rhs);

				condLength = stream.read();
				if (condLength == 0) {
					condition = null;
				} else {
					astString = stream.readUTF();
					condition = engine.parse(astString);
					pmEvaluator.setCondition(condition);
				}
				addSimplePatternRule(lhs, pmEvaluator);
			}

		}

		len = stream.read();
		if (len > 0) {
			fPatternRules = new ArrayList<IPatternMatcher<IExpr>>();
			listLength = stream.read();
			for (int j = 0; j < listLength; j++) {
				astString = stream.readUTF();
				setSymbol = F.symbol(astString);

				astString = stream.readUTF();
				lhs = engine.parse(astString);
				astString = stream.readUTF();
				rhs = engine.parse(astString);
				pmEvaluator = new PatternMatcherAndEvaluator(setSymbol, lhs, rhs);

				condLength = stream.read();
				if (condLength == 0) {
					condition = null;
				} else {
					astString = stream.readUTF();
					condition = engine.parse(astString);
					pmEvaluator.setCondition(condition);
				}
				addSimplePatternRule(lhs, pmEvaluator);
			}
		}
	}

	public void writeSymbol(java.io.ObjectOutputStream stream) throws java.io.IOException {
		Iterator<IExpr> iter;
		IExpr key;
		IExpr condition;
		Pair<ISymbol, IExpr> pair;
		ISymbol setSymbol;
		PatternMatcherAndEvaluator pmEvaluator;
		if (fEqualRules == null || fEqualRules.size() == 0) {
			stream.write(0);
		} else {
			stream.write(fEqualRules.size());
			iter = fEqualRules.keySet().iterator();
			while (iter.hasNext()) {
				key = iter.next();
				pair = fEqualRules.get(key);
				stream.writeUTF(pair.getFirst().toString());
				stream.writeUTF(key.fullFormString());
				stream.writeUTF(pair.getSecond().fullFormString());
			}
		}
		if (fSimplePatternRules == null || fSimplePatternRules.size() == 0) {
			stream.write(0);
		} else {
			stream.write(fSimplePatternRules.size());
			Iterator<IPatternMatcher<IExpr>> listIter = fSimplePatternRules.values().iterator();
			IPatternMatcher<IExpr> elem;
			while (listIter.hasNext()) {
				elem = listIter.next();
				pmEvaluator = (PatternMatcherAndEvaluator) elem;
				setSymbol = pmEvaluator.getSetSymbol();
				stream.writeUTF(setSymbol.toString());
				stream.writeUTF(pmEvaluator.getLHS().fullFormString());
				stream.writeUTF(pmEvaluator.getRHS().fullFormString());
				condition = pmEvaluator.getCondition();
				if (condition == null) {
					stream.write(0);
				} else {
					stream.write(1);
					stream.writeUTF(condition.fullFormString());
				}
			}
		}
		if (fPatternRules == null || fPatternRules.size() == 0) {
			stream.write(0);
		} else {
			stream.write(fPatternRules.size());

			for (int i = 0; i < fPatternRules.size(); i++) {
				pmEvaluator = (PatternMatcherAndEvaluator) fPatternRules.get(i);
				setSymbol = pmEvaluator.getSetSymbol();
				stream.writeUTF(setSymbol.toString());
				stream.writeUTF(pmEvaluator.getLHS().fullFormString());
				stream.writeUTF(pmEvaluator.getRHS().fullFormString());
				condition = pmEvaluator.getCondition();
				if (condition == null) {
					stream.write(0);
				} else {
					stream.write(1);
					stream.writeUTF(condition.fullFormString());
				}
			}

		}
	}
}