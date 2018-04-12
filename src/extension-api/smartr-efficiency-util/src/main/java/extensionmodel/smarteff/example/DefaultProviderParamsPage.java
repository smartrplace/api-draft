package extensionmodel.smarteff.example;

import org.smartrplace.smarteff.util.EditPageGeneric;

public class DefaultProviderParamsPage extends EditPageGeneric<DefaultProviderParams> {
	@Override
	public void setData() {
		setLabel("basePriceBuildingAnalysis", EN, "Price component per building independently of size",
				DE, "Grundpreis pro Gebäude");
		setLabel("pricePerSQMBuildingAnalysis", EN, "Additional price per square meter",
				DE, "Zusätzliche Preiskomponente in EUR/m2");
		setLabel("costOfCustomerHour", DE, "Stundensatz Kunde", EN, "Customer EUR/h", 0, 9999);
		setLabel("defaultKwhPerSQM", EN, "base assumption for consumption kWh/sqm/a");
	}
	@Override
	protected Class<DefaultProviderParams> typeClass() {
		return DefaultProviderParams.class;
	}
}
