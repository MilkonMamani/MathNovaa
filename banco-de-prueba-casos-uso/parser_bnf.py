"""Parser de descenso recursivo que sigue la gramatica BNF de MathNova, para validarla."""
import re, csv, random, sys
from sympy import Integer, Rational, Symbol, sqrt, cbrt, sin, cos, tan, cot, sec, csc, asin, acos, atan, log, exp, E, pi, I, N

KEYWORDS = sorted(['arcsen', 'arcsin', 'arccos', 'arctan', 'arctg', 'cosec', 'sen', 'sin', 'cos', 'tan', 'tg',
                   'cot', 'ctg', 'sec', 'csc', 'ln', 'log', 'sqrt', 'exp', 'pi'], key=len, reverse=True)
FUN = {'sen': sin, 'sin': sin, 'cos': cos, 'tan': tan, 'tg': tan, 'cot': cot, 'ctg': cot, 'sec': sec, 'csc': csc,
       'cosec': csc, 'arcsen': asin, 'arcsin': asin, 'arccos': acos, 'arctan': atan, 'arctg': atan,
       'ln': log, 'sqrt': sqrt, 'exp': exp}
VARS = {'x', 'y', 'z'}
CONST = {'e': E, 'i': I, 'π': pi, 'pi': pi}

class ErrorSintaxis(Exception):
    pass

def lex(s):
    s = s.replace('−', '-').replace('×', '*').replace('²', '^2').replace('³', '^3')
    toks, i = [], 0
    while i < len(s):
        ch = s[i]
        if ch.isspace():
            i += 1; continue
        m = re.match(r'\d+(\.\d+)?', s[i:])
        if m:
            toks.append(('NUM', m.group())); i += len(m.group()); continue
        if ch.isalpha() and ch != 'π':
            for kw in KEYWORDS:
                if s.startswith(kw, i):
                    toks.append(('ID', kw)); i += len(kw); break
            else:
                if ch in VARS:
                    toks.append(('VAR', ch))
                elif ch in ('e', 'i'):
                    toks.append(('CONST', ch))
                else:
                    raise ErrorSintaxis(f"'{ch}' en la posición {i + 1} no es una variable permitida (x, y, z)")
                i += 1
            continue
        if ch == 'π':
            toks.append(('CONST', 'π')); i += 1; continue
        if ch in '+-*/^()·√∛_':
            toks.append(('OP', ch)); i += 1; continue
        raise ErrorSintaxis(f"carácter '{ch}' no reconocido en la posición {i + 1}")
    return toks

class Parser:
    def __init__(self, toks):
        self.t, self.k = toks, 0
    def peek(self):
        return self.t[self.k] if self.k < len(self.t) else ('EOF', '')
    def take(self, typ=None, val=None):
        tok = self.peek()
        if (typ and tok[0] != typ) or (val and tok[1] != val):
            raise ErrorSintaxis(f'se esperaba {val or typ} y llegó {tok[1] or "fin"}')
        self.k += 1
        return tok
    def is_op(self, *vals):
        tok = self.peek()
        return tok[0] == 'OP' and tok[1] in vals

    # <expresion> ::= <termino> { ("+" | "-") <termino> }
    def expresion(self):
        v = self.termino()
        while self.is_op('+', '-'):
            op = self.take()[1]
            w = self.termino()
            v = v + w if op == '+' else v - w
        return v

    # <termino> ::= <unario> { <op_mult> <unario> | <factor_implicito> }
    def termino(self):
        v = self.unario()
        while True:
            if self.is_op('*', '·', '/'):
                op = self.take()[1]
                w = self.unario()
                v = v / w if op == '/' else v * w
            elif self.inicia_implicito():
                v = v * self.potencia()
            else:
                return v

    def inicia_implicito(self):
        tok = self.peek()
        return tok[0] in ('VAR', 'CONST', 'ID') or (tok[0] == 'OP' and tok[1] in '(√∛')

    # <unario> ::= ("+" | "-") <unario> | <potencia>
    def unario(self):
        if self.is_op('-'):
            self.take(); return -self.unario()
        if self.is_op('+'):
            self.take(); return self.unario()
        return self.potencia()

    # <potencia> ::= <primario> [ "^" <exponente> ]
    def potencia(self):
        b = self.primario()
        if self.is_op('^'):
            self.take(); return b ** self.exponente()
        return b

    # <exponente> ::= [ "+" | "-" ] <primario> [ "^" <exponente> ]
    def exponente(self):
        sg = 1
        if self.is_op('-', '+'):
            sg = -1 if self.take()[1] == '-' else 1
        b = self.primario()
        if self.is_op('^'):
            self.take(); b = b ** self.exponente()
        return sg * b

    # <primario> ::= <numero> | <variable> | <constante> | "(" <expresion> ")" | <llamada> | <raiz>
    def primario(self):
        tok = self.peek()
        if tok[0] == 'NUM':
            self.take(); return Rational(tok[1])
        if tok[0] == 'VAR':
            self.take(); return Symbol(tok[1])
        if tok[0] == 'CONST' or (tok[0] == 'ID' and tok[1] == 'pi'):
            self.take(); return CONST[tok[1]]
        if self.is_op('('):
            self.take(); v = self.expresion(); self.take('OP', ')'); return v
        if self.is_op('√', '∛'):
            r = self.take()[1]; a = self.primario()
            return sqrt(a) if r == '√' else cbrt(a)
        if tok[0] == 'ID':
            return self.llamada()
        raise ErrorSintaxis(f'se esperaba un número, una variable o "(" y llegó {tok[1] or "fin"}')

    # <llamada> ::= <funcion> [ "^" <entero> ] <argumento>
    # <funcion> ::= ... | "log" [ "_" <entero> ]
    def llamada(self):
        name = self.take('ID')[1]
        base = None
        if name == 'log':
            base = 10
            if self.is_op('_'):
                self.take(); base = int(self.take('NUM')[1])
        pot = None
        if self.is_op('^'):
            self.take(); pot = Integer(self.take('NUM')[1])
        a = self.argumento()
        v = log(a, base) if name == 'log' else FUN[name](a)
        return v ** pot if pot is not None else v

    # <argumento> ::= "(" <expresion> ")" | <argumento_simple>
    # <argumento_simple> ::= [ <numero> ] <variable> [ "^" <entero> ]
    def argumento(self):
        if self.is_op('('):
            self.take(); v = self.expresion(); self.take('OP', ')'); return v
        c = 1
        if self.peek()[0] == 'NUM':
            c = Rational(self.take()[1])
        v = Symbol(self.take('VAR')[1])
        if self.is_op('^'):
            self.take(); v = v ** Integer(self.take('NUM')[1])
        return c * v

def parse(s):
    p = Parser(lex(s))
    v = p.expresion()
    if p.peek()[0] != 'EOF':
        raise ErrorSintaxis(f'sobra "{p.peek()[1]}" al final')
    return v

if __name__ == '__main__':
    sys.path.insert(0, '/home/claude/banco')
    import importlib.util
    from sympy.parsing.sympy_parser import parse_expr
    spec = importlib.util.spec_from_file_location('g', '/home/claude/banco/gen.py')
    # reutiliza el lector de referencia del generador sin volver a generar
    src = open('/home/claude/banco/gen.py').read().split('# ------------------------------------------------------------------- casos')[0]
    ns = {}
    exec(src, ns)
    parse_ms, same = ns['parse_ms'], ns['same']
    ok = bad = 0
    for r in csv.DictReader(open('/home/claude/banco/banco_factorizacion.csv', encoding='utf-8')):
        for col in ('entrada', 'resultado_esperado'):
            txt = r[col].split(';')[0]
            if txt.startswith('Irreducible'):
                continue
            try:
                a = parse(txt)
                assert same(a, parse_ms(txt)), 'valor distinto'
                ok += 1
            except Exception as ex:
                bad += 1
                print('FALLA', r['id'], col, txt, '->', ex)
    print('ok', ok, 'fallas', bad)
    extras = {'sen 2x': sin(2*Symbol('x')), 'sen^2(x)': sin(Symbol('x'))**2, 'sen x cos x': sin(Symbol('x'))*cos(Symbol('x')),
              'log_2(x)': log(Symbol('x'), 2), 'log x': log(Symbol('x'), 10), '2x^2y': 2*Symbol('x')**2*Symbol('y'),
              '-x^2': -Symbol('x')**2, 'x^-1': 1/Symbol('x'), '2^3^2': Integer(512), 'e^2x': E**2*Symbol('x'),
              'x²+3x': Symbol('x')**2 + 3*Symbol('x'), '3(x+1)(x−2)': 3*(Symbol('x')+1)*(Symbol('x')-2),
              'arcsen(x)+cosec x': asin(Symbol('x')) + csc(Symbol('x')), '0.5x': Symbol('x')/2, 'πx': pi*Symbol('x')}
    for k, v in extras.items():
        got = parse(k)
        print(('OK  ' if (got - v).simplify() == 0 else 'MAL ') + k, '=>', got)
    for k in ['2x +', 'x^', '3a + 1', 'sen', '(x+1', 'x # 2', '2 3']:
        try:
            parse(k); print('ACEPTÓ (no debía)', k)
        except ErrorSintaxis as ex:
            print('rechaza', repr(k), '->', ex)
