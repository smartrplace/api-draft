package extensionmodel.smarteff.example;

import org.smartrplace.smarteff.util.EditPageGeneric;

public class DefaultProviderParamsPage extends EditPageGeneric<DefaultProviderParams> {
	@Override
	public void setData(DefaultProviderParams sr) {
		setLabel(sr.basePriceBuildingAnalysis(), EN, "Price component per building independently of size",
				DE, "Grundpreis pro Gebäude");
		setLabel(sr.pricePerSQMBuildingAnalysis(), EN, "Additional price per square meter",
				DE, "Zusätzliche Preiskomponente in EUR/m2");
		setLabel(sr.costOfCustomerHour(), DE, "Stundensatz Kunde", EN, "Customer EUR/h", 0, 9999);
		setLabel(sr.defaultKwhPerSQM(), EN, "base assumption for consumption kWh/sqm/a");
	}
	@Override
	protected Class<DefaultProviderParams> primaryEntryTypeClass() {
		return DefaultProviderParams.class;
	}
}
