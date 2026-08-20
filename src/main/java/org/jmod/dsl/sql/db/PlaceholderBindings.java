package org.jmod.dsl.sql.db;

import java.util.ArrayList;
import java.util.List;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.Values;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import net.sf.jsqlparser.util.TablesNamesFinder;

/**
 * JDBC placeholders in source order, each optionally bound to a compared/assigned column.
 */
final class PlaceholderBindings extends TablesNamesFinder<Void> {
    private final List<Column> columns = new ArrayList<>();
    private Column current;

    private PlaceholderBindings() {
    }

    static List<Column> columns(Statement statement) {
        PlaceholderBindings binder = new PlaceholderBindings();
        binder.getTables(statement);
        return binder.columns;
    }

    @Override
    public <S> Void visit(JdbcParameter parameter, S context) {
        columns.add(current);
        return null;
    }

    @Override
    public void visitBinaryExpression(BinaryExpression expression) {
        Column previous = current;
        Column column = columnOf(expression.getLeftExpression());
        if (column == null) {
            column = columnOf(expression.getRightExpression());
        }
        if (column != null) {
            current = column;
        }
        super.visitBinaryExpression(expression);
        current = previous;
    }

    @Override
    public <S> Void visit(InExpression expression, S context) {
        if (expression.getLeftExpression() != null) {
            expression.getLeftExpression().accept(this, context);
        }
        Column previous = current;
        Column column = columnOf(expression.getLeftExpression());
        Expression right = expression.getRightExpression();
        current = column != null && isValueList(right) ? column : null;
        if (right != null) {
            right.accept(this, context);
        }
        current = previous;
        return null;
    }

    @Override
    public <S> Void visit(Between expression, S context) {
        Column previous = current;
        Column column = columnOf(expression.getLeftExpression());
        if (column != null) {
            current = column;
        }
        if (expression.getLeftExpression() != null) {
            expression.getLeftExpression().accept(this, context);
        }
        if (expression.getBetweenExpressionStart() != null) {
            expression.getBetweenExpressionStart().accept(this, context);
        }
        if (expression.getBetweenExpressionEnd() != null) {
            expression.getBetweenExpressionEnd().accept(this, context);
        }
        current = previous;
        return null;
    }

    @Override
    public <S> Void visit(Insert insert, S context) {
        ExpressionList<Column> insertColumns = insert.getColumns();
        Values values = insert.getValues();
        if (insertColumns != null && values != null && values.getExpressions() != null) {
            bindPaired(insertColumns, values.getExpressions(), context);
            if (insert.getTable() != null) {
                visit(insert.getTable(), context);
            }
            return null;
        }
        return super.visit(insert, context);
    }

    @Override
    public <S> Void visit(Update update, S context) {
        if (update.getUpdateSets() != null) {
            for (UpdateSet set : update.getUpdateSets()) {
                bindPaired(set.getColumns(), set.getValues(), context);
            }
        }
        Column previous = current;
        current = null;
        if (update.getWhere() != null) {
            update.getWhere().accept(this, context);
        }
        current = previous;
        if (update.getTable() != null) {
            visit(update.getTable(), context);
        }
        return null;
    }

    private <S> void bindPaired(ExpressionList<Column> columns, ExpressionList<?> values, S context) {
        if (columns == null || values == null) {
            return;
        }
        int n = Math.min(columns.size(), values.size());
        for (int i = 0; i < n; i++) {
            Column previous = current;
            current = columns.get(i);
            Expression value = values.get(i);
            if (value != null) {
                value.accept(this, context);
            }
            current = previous;
        }
    }

    private static boolean isValueList(Expression expression) {
        return expression instanceof ExpressionList && !(expression instanceof Select);
    }

    private static Column columnOf(Expression expression) {
        Expression current = expression;
        while (current instanceof Parenthesis parenthesis) {
            current = parenthesis.getExpression();
        }
        return current instanceof Column column ? column : null;
    }
}
