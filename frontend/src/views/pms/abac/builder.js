import { createEmptyGroup, ruleNodeToSpel, spelToRuleNode } from '@zeng-alt/vue-spel-query-builder'

// 词法归一化仅供规则树解析；字符串字面量保持原值。
export function normalizeOperators(expression) {
  return expression.replace(/('(?:''|\\.|[^'])*'|"(?:\\.|[^"])*")|\b(and|or|not)\b/g, (match, literal, operator, index) => {
    if (literal || /[.#\w]/.test(expression[index - 1] || ''))
      return match
    return { and: '&&', or: '||', not: '!' }[operator]
  })
}

function comparable(expression) {
  const tokens = normalizeOperators(expression).match(/'(?:''|\\.|[^'])*'|"(?:\\.|[^"])*"|&&|\|\||==|!=|>=|<=|\S/g) || []
  while (tokens[0] === '(' && tokens.at(-1) === ')') {
    let depth = 0
    const closesEarly = tokens.slice(0, -1).some((token) => {
      if (token === '(')
        depth++
      if (token === ')')
        depth--
      return depth === 0
    })
    if (closesEarly)
      break
    tokens.shift()
    tokens.pop()
  }
  return tokens.join(' ')
}

export function parseVisualRule(expression) {
  if (!expression.trim())
    return createEmptyGroup('and')
  const rule = spelToRuleNode(normalizeOperators(expression))
  if (comparable(ruleNodeToSpel(rule)) !== comparable(expression))
    throw new Error('该表达式暂不能完整转换为规则树，请继续使用代码编辑；原表达式已保留。')
  return rule
}

export { ruleNodeToSpel }
