package dubstep.executor;

import dubstep.utils.Evaluator;
import dubstep.utils.Tuple;
import net.sf.jsqlparser.expression.PrimitiveValue;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static dubstep.planner.PlanTree.getSelectExprColumnStrList;

public class ProjNode extends BaseNode {

    private List<SelectItem> selectItems; //used for building of projection info
    private ArrayList<SelectExpressionItem> selectExpressionItems = new ArrayList<>();
    private List<String> completeProjectionTables = new ArrayList<>();
    private Evaluator eval;
    //private BaseNode resNode;
    //private ArrayList<Expression> expressions = new ArrayList<Expression>();

    public ProjNode(List<SelectItem> selectItems, BaseNode InnerNode) {
        super();
        this.innerNode = InnerNode;
        this.selectItems = selectItems;
        this.innerNode.parentNode = this;

        for (SelectItem item : selectItems) {
            if (item instanceof AllTableColumns) {
                completeProjectionTables.add(((AllTableColumns) item).getTable().getName());
            } else if (item instanceof SelectExpressionItem) {
                selectExpressionItems.add((SelectExpressionItem) item);
            } else {
                throw new UnsupportedOperationException("We don't support this select operation - " + item.toString());
            }
        }

//        for (SelectExpressionItem expressionItems:selectExpressionItems){
//            expressions.add(expressionItems.getExpression());
//        }
        initProjectionInfo();
        eval = new Evaluator(innerNode.projectionInfo);
    }

    @Override
    Tuple getNextRow() {
        PrimitiveValue[] values = new PrimitiveValue[this.selectExpressionItems.size()];
        List<ColumnDefinition> colDefs = new ArrayList<>();
        Tuple nextRow = this.innerNode.getNextTuple();
        if (nextRow == null)
            return null;
        eval.setTuple(nextRow);
        int index = 0;
        for (SelectExpressionItem expression : this.selectExpressionItems) {
            try {
                PrimitiveValue value = eval.eval(expression.getExpression());
                values[index] = value;
                index++;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return new Tuple(values);
    }

    @Override
    void resetIterator() {
        innerNode.resetIterator();
    }

    @Override
    void initProjectionInfo() {
        projectionInfo = new HashMap<>();
        typeList = new ArrayList<>();
        for (int i = 0; i < selectExpressionItems.size(); ++i) {
            SelectItem selectItem = selectExpressionItems.get(i);
            String columnName = ((SelectExpressionItem) selectItem).getExpression().toString();
            String alias = ((SelectExpressionItem) selectItem).getAlias();

            if (((SelectExpressionItem) selectItem).getExpression() instanceof Column)
                typeList.add(innerNode.typeList.get(innerNode.projectionInfo.get(columnName)));
            else
                typeList.add(null);
            if (alias == null) {
                projectionInfo.put(columnName, i);
            } else {
                projectionInfo.put(alias, i);
            }
        }
    }

    @Override
    public void initProjPushDownInfo() {
        if (this.parentNode != null)
            requiredList.addAll(this.parentNode.requiredList);
        for (int i = 0; i < selectExpressionItems.size(); ++i) {
            requiredList.addAll(getSelectExprColumnStrList(selectExpressionItems.get(i).getExpression()));
        }
        this.innerNode.initProjPushDownInfo();
    }
}
