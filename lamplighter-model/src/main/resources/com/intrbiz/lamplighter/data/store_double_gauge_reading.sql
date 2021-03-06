CREATE OR REPLACE FUNCTION "lamplighter"."store_double_gauge_reading"("p_site_id" UUID, "p_reading_id" UUID, "p_collected_at" TIMESTAMP WITH TIME ZONE, "p_value" DOUBLE PRECISION, "p_warning" DOUBLE PRECISION, "p_critical" DOUBLE PRECISION, "p_min" DOUBLE PRECISION, "p_max" DOUBLE PRECISION)
RETURNS SETOF INTEGER AS
$BODY$
DECLARE
  v_table TEXT;
BEGIN
  v_table := quote_ident(lamplighter.get_schema(p_site_id)) || '.' || quote_ident(lamplighter.get_table_name('double_gauge_reading', p_reading_id));
  EXECUTE $$INSERT INTO $$ || v_table || $$ ("site_id", "reading_id", "collected_at", "value", "warning", "critical", "min", "max") 
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8)$$
   USING p_site_id, p_reading_id, p_collected_at, p_value, p_warning, p_critical, p_min, p_max;
  RETURN NEXT 1;
  RETURN;
END;
$BODY$
LANGUAGE plpgsql;